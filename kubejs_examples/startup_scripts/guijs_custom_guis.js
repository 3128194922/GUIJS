// ============================================================
// GUIJS 示例：tiny_chest（红石感知单格箱）与 mini_furnace（真熔炉）
// （startup_scripts，需重启游戏；需使用补丁版 KubeJS 才有红石/比较器/生命周期 API）
//
// tiny_chest   —— 容器级过滤（漏斗同规）、比较器按数量输出、生命周期日志
// mini_furnace —— 查询原版熔炼配方（任意可熔炼物品）、真实燃料热值、
//                 红石信号停机、输出槽禁放入、烧制进度条 + 燃料动态文本
// ============================================================

// --- 熔炉配方查询工具（vanilla SMELTING，KubeJS Java interop）---
let RecipeType = Java.loadClass('net.minecraft.world.item.crafting.RecipeType')
let SimpleContainer = Java.loadClass('net.minecraft.world.SimpleContainer')
let SMELT_TEST = new SimpleContainer(1)   // 复用的匹配测试容器

/** 查询物品的原版熔炼产物；不可熔炼返回 null */
function smeltingResult(level, input) {
    if (input.isEmpty()) {
        return null
    }
    SMELT_TEST.clearContent()
    SMELT_TEST.setItem(0, input)
    let holder = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, SMELT_TEST, level).orElse(null)
    return holder == null ? null : holder.value().getResultItem(level.registryAccess())
}

/** 输出槽能否接收产物（空槽或同类未堆满） */
function canInsertOutput(inv, result) {
    let out = inv.getStackInSlot(2)
    return out.isEmpty() || (out.id == result.id && out.count + result.count <= out.maxStackSize)
}

// ============================================================
// GUI 定义（模块化锚点布局）
// ============================================================

GuiJS.createGui('tiny_chest', gui => {
    // 布局照抄原版 1 行箱子（176x132）：容器槽 y=20、背包 y=50、快捷栏 y=108
    gui.title('单格箱子')
    gui.size(176, 132)

    // 单格容器槽：水平居中（x=0 即正中），距标题下方 20px，与背包区域互不干扰
    gui.anchor('center', 'top')
    gui.slot(0, 0, 20)
        .filter('minecraft:iron_ore')
        .maxStackSize(16)
        .onChange(event => {
            if (!event.stack.isEmpty()) {
                event.player.tell(`箱子内容: ${event.stack.id} x${event.stack.count}`)
            }
        })

    // 玩家背包显式定位（原版位置），"Inventory" 标题自动显示在背包上方
    gui.anchor('left', 'top')
    gui.playerInventory(8, 50)
})

GuiJS.createGui('mini_furnace', gui => {
    // 布局照抄原版熔炉（176x166）：
    // 输入(56,17) 燃料(56,53) 输出(116,35) 进度条(79,34) 背包(8,84) 快捷栏(8,142)
    gui.title('JS 熔炉')
    gui.size(176, 166)

    gui.slot(0, 56, 17)                                       // 输入槽（任何物品，tick 里查配方）
    gui.slot(1, 56, 53)                                       // 燃料槽
        .filter(stack => !stack.isEmpty() && stack.getBurnTime(null) > 0)  // 真实燃料过滤
    gui.slot(2, 116, 35)                                      // 输出槽
        .filter(stack => false)                               // 原生熔炉行为：只许取出，禁止放入
        .onTake(event => event.player.tell(`取出产物: ${event.stack.id} x${event.stack.count}`))

    // 烧制进度条：原版箭头位置（输入槽与输出槽之间，水平填充）
    gui.progress('cook', 79, 34, 24, 16, 0)                   // ← data.guijs0（0..10000）

    // 燃料余量动态文本：燃料槽正下方（避开左侧 "Inventory" 标题区）
    gui.dataLabel(60, 72, 1, '燃料: ')                         // ← data.guijs1

    // 点火按钮：面板右上角（与标题同行，避开全部槽位区域）
    gui.button('ignite', 124, 3, 46, 15, '点火', event => {
        if (event.pos != null) {
            const be = event.player.level.getBlockEntity(event.pos)
            be.data.burnLeft = Math.max(be.data.getInt('burnLeft'), 200)   // 手动点火 10 秒
            be.save()
            be.updateNeighbors()
            event.player.tell('熔炉已点火！')
        }
    })

    // 玩家背包：显式原版位置（8,84），"Inventory" 标题自动显示在 y=73
    gui.playerInventory(8, 84)
})

// ============================================================
// 方块注册
// ============================================================

StartupEvents.registry('block', event => {

    // --------------------------------------------------------
    // 单格箱子（红石感知）
    // --------------------------------------------------------
    event.create('tiny_chest', 'basic')
        .hardness(1.5)
        .blockEntity(info => {
            // 容器级过滤：GUI / JS / 漏斗管道 三条插入路径统一只收铁矿石
            info.inventory(1, 1, Ingredient.of('minecraft:iron_ore'))

            // 比较器按物品数量输出（0..15）—— 放红石比较器即可读取
            info.comparatorOutput(entity => {
                const stack = entity.inventory.getStackInSlot(0)
                return stack.isEmpty() ? 0 : Math.min(15, Math.floor(stack.count / 16 * 15) + 1)
            })

            // 生命周期（原生 onLoad / onChunkUnloaded）
            info.onLoad(entity => console.info(`[tiny_chest] 已加载 @ ${entity.x},${entity.y},${entity.z}`))
            info.onUnload(entity => console.info(`[tiny_chest] 已卸载 @ ${entity.x},${entity.y},${entity.z}`))
        })

    // --------------------------------------------------------
    // 真熔炉：任意可熔炼物品 + 真实燃料 + 红石停机
    // --------------------------------------------------------
    event.create('mini_furnace', 'basic')
        .hardness(2.0)
        .blockEntity(info => {
            info.inventory(1, 3)   // 0=输入 1=燃料 2=输出

            // 比较器输出烧制进度（0..15）
            info.comparatorOutput(entity => Math.floor(entity.data.getInt('cookTime') / 200 * 15))

            // 生命周期
            info.onLoad(entity => console.info(`[mini_furnace] 已加载 @ ${entity.x},${entity.y},${entity.z}`))
            info.onUnload(entity => console.info(`[mini_furnace] 已卸载 @ ${entity.x},${entity.y},${entity.z}`))

            // 邻居变化（含红石）：有红石信号时停机（Java 机器惯例）
            info.neighborChanged((entity, fromPos) => {
                const level = entity.getBlock().getLevel()
                const was = entity.data.getBoolean('redstoneOff')
                entity.data.redstoneOff = level.getBestNeighborSignal(entity.getBlock().pos) > 0
                if (was !== entity.data.getBoolean('redstoneOff')) {
                    entity.save()
                    entity.updateNeighbors()   // 通知红石网络刷新
                }
            })

            // 主逻辑：每 4 tick
            info.serverTick(4, 0, entity => {
                const inv = entity.inventory
                const data = entity.data
                const level = entity.getBlock().getLevel()
                const input = inv.getStackInSlot(0)
                let burn = data.getInt('burnLeft')

                // 消耗燃烧
                if (burn > 0) {
                    data.burnLeft = burn - 4
                }

                // GUI 动态数据桥：guijs0=烧制进度(0..10000) guijs1=燃料余量
                data.guijs0 = Math.floor(data.getInt('cookTime') / 200 * 10000)
                data.guijs1 = data.getInt('burnLeft')

                const result = smeltingResult(level, input)   // 真实配方查询
                const stopped = data.getBoolean('redstoneOff')

                if (!stopped && result != null && burn > 0) {
                    // 燃烧中且有可熔炼输入 → 烧制推进
                    if (canInsertOutput(inv, result)) {
                        let cook = data.getInt('cookTime') + 4
                        data.cookTime = cook

                        if (cook >= 200) {
                            data.cookTime = 0
                            inv.extractItem(0, 1, false)      // 消耗输入

                            let out = inv.getStackInSlot(2)
                            if (out.isEmpty()) {
                                inv.setItem(2, result.copy())
                            } else {
                                out.count += result.count
                            }
                        }
                    } else {
                        data.cookTime = 0   // 输出已满，进度回退
                    }
                } else if (!stopped && result != null && burn <= 0) {
                    // 有可熔炼输入但没火了 → 自动补燃料（按真实热值）
                    const fuel = inv.getStackInSlot(1)
                    const burnTime = fuel.isEmpty() ? 0 : fuel.getBurnTime(null)
                    if (burnTime > 0 && canInsertOutput(inv, result)) {
                        data.burnLeft = burnTime
                        inv.extractItem(1, 1, false)
                    } else {
                        data.cookTime = 0
                    }
                } else if (result == null) {
                    data.cookTime = 0   // 输入不可熔炼
                }

                entity.save()
            })
        })
})

// 右键绑定：直接打开对应 GUI
GuiJS.bindBlock('kubejs:tiny_chest', 'tiny_chest')
GuiJS.bindBlock('kubejs:mini_furnace', 'mini_furnace')
