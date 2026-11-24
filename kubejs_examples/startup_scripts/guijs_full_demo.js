// ============================================================
// GUIJS 全功能演示（startup_scripts，需重启游戏）
// 涵盖：KubeJS 上游补丁新能力（红石/比较器/生命周期）
//      + GUIJS 自定义 GUI（锚点布局/槽位规则/按钮回调）
//      + 方块实体原生能力（漏斗管道/破坏掉落，自动生效）
// ============================================================

StartupEvents.registry('block', event => {

    // --------------------------------------------------------
    // 1. 红石机器 —— KubeJS 上游补丁新能力完整演示
    //    感知红石 → 亮起并输出红石 → 比较器读取容器 → 生命周期日志
    // --------------------------------------------------------
    event.create('redstone_machine', 'basic')
        .hardness(2.0)
        .blockEntity(info => {
            info.inventory(1, 2, Ingredient.of('#forge:ingots'))   // 2 格，漏斗只进锭
            info.enableSync()

            // 【新能力 1】邻居变化感知（原生 Block#neighborChanged）
            // 任何邻居方块/红石信号变化都会触发；用 getBestNeighborSignal 读红石输入
            info.neighborChanged((entity, fromPos) => {
                const level = entity.getBlock().getLevel()
                const pos = entity.getBlock().pos
                const was = entity.data.powered
                entity.data.powered = level.getBestNeighborSignal(pos) > 0

                if (was !== entity.data.powered) {
                    entity.save()
                    // 【新能力 2】红石输出变化后必须通知邻居（原生 updateNeighbourForOutputSignal）
                    entity.updateNeighbors()
                }
            })

            // 【新能力 3】作为红石电源输出信号（原生 Block#getSignal / isSignalSource）
            // 机器"通电"时向所有方向输出 15 级红石，像红石块
            info.signal((entity, direction) => entity.data.powered ? 15 : 0)

            // 【新能力 4】比较器读取（原生 Block#getAnalogOutputSignal）
            // 按容器占有格数输出 0-15
            info.comparatorOutput(entity => {
                const inv = entity.inventory
                let filled = 0
                for (let i = 0; i < inv.getContainerSize(); i++) {
                    if (!inv.getStackInSlot(i).isEmpty()) filled++
                }
                return Math.floor(filled / inv.getContainerSize() * 15)
            })

            // 【新能力 5】生命周期回调（原生 onLoad / onChunkUnloaded）
            info.onLoad(entity => console.info(`[demo] 红石机器加载 @ ${entity.x},${entity.y},${entity.z}`))
            info.onUnload(entity => console.info(`[demo] 红石机器卸载 @ ${entity.x},${entity.y},${entity.z}`))

            // 红石机器 GUI 的进度数据（GUI 端 dataLabel 使用）
            info.serverTick(20, 0, entity => {
                entity.data.guijs0 = entity.data.powered ? 10000 : 0
            })
        })

    // --------------------------------------------------------
    // 2. JS 木桶 —— 漏斗/管道兼容 + 破坏掉落 + 比较器（GUIJS 自动补齐）
    // --------------------------------------------------------
    event.create('js_barrel', 'basic')
        .hardness(1.5)
        .blockEntity(info => {
            // 3x4=12 格：0-8 主容器 + 9 为锁定奖励槽；inputFilter 三条路径统一只收原木
            info.inventory(3, 4, Ingredient.of('#minecraft:logs'))

            // 比较器按容器满度输出
            info.comparatorOutput(entity => {
                const inv = entity.inventory
                let count = 0
                for (let i = 0; i < inv.getContainerSize(); i++) {
                    count += inv.getStackInSlot(i).count
                }
                return Math.min(15, Math.floor(count / 36 * 15 * 3))
            })
        })

    // --------------------------------------------------------
    // 3. 注册方块完成 —— 右键绑定 GUI（见下方 GUI 定义）
    // --------------------------------------------------------
})

// ============================================================
// 自定义 GUI 定义（模块化锚点布局 + Java 级槽位控制）
// ============================================================

// 红石机器状态面板：动态文本 + 进度条 + 手动槽位
GuiJS.createGui('redstone_machine', gui => {
    gui.title('红石机器')
    gui.size(176, 120)

    gui.anchor('center', 'top')
    gui.grid(0, 1, 2, 0, 20)                            // 容器槽 0-1（只收锭）
    gui.progress('power', 0, 62, 100, 12, 0)            // 供电状态（data.guijs0）
    gui.dataLabel(0, 80, 0, '比较器输出基数: ')          // 动态数值文本

    gui.playerInventory()
})

// JS 木桶 GUI：槽位过滤 + 锁定奖励槽 + 取出回调
GuiJS.createGui('js_barrel', gui => {
    gui.title('JS 木桶')
    gui.size(176, 150)

    gui.anchor('center', 'top')
    gui.grid(0, 3, 3, 0, 18)                            // 9 格主容器
        .filter(Ingredient.of('#minecraft:logs'))       // 与 inputFilter 一致

    gui.slot(9, 0, 78)                                  // 奖励槽（容器索引 9 需扩展容器；此处演示锁定槽）
        .locked()
        .onTake(event => {
            event.player.give('minecraft:emerald')
            event.player.tell('领取了奖励！')
        })

    gui.dataLabel(0, 100, 0, '桶内物品数: ')
    gui.playerInventory()
})

// 管理面板：按钮直接绑定 JS 函数
GuiJS.createGui('admin_panel', gui => {
    gui.title('管理面板')
    gui.size(200, 110)
    gui.anchor('center', 'top')
    gui.button('heal', 0, 25, 80, 18, '回满血', event => {
        event.player.health = event.player.maxHealth
    })
    gui.button('day', 0, 50, 80, 18, '设置白天', event => {
        event.player.server.overworld.dayTime = 1000
    })
    gui.button('close', 0, 75, 80, 18, '关闭', event => {
        event.player.closeContainer()
    })
})

// 右键绑定：直接打开对应 GUI
GuiJS.bindBlock('kubejs:redstone_machine', 'redstone_machine')
GuiJS.bindBlock('kubejs:js_barrel', 'js_barrel')
