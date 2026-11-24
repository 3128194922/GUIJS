# GUIJS

Minecraft 1.20.1 Forge 的 KubeJS 附加模组：让 KubeJS 脚本可以**新建并调用打开 GUI 界面** ——
原版全部 GUI、所有已安装模组注册的容器菜单、以及**完全由脚本定义的自定义 GUI**
（背景 / 大小 / 槽位 / 按钮 / 进度条 / 文本 / 贴图元素，位置自由设定）。

- modid：`guijs`，绑定名：`GuiJS`
- 依赖：KubeJS 2001.6.4+（对应 `其他项目/KubeJS-2001` 源码的 2001.6.5 版本线）

## 安装

1. 编译：`gradlew build`，产物在 `build/libs/guijs-1.0.0.jar`
2. 将 GUIJS 与 KubeJS 一起放入 `mods/`
3. 脚本放入游戏实例 `kubejs/startup_scripts/` 与 `kubejs/server_scripts/`
   （示例见 `kubejs_examples/`），startup 脚本需重启游戏，server 脚本 `/reload` 生效

## 一、打开已有的 GUI

### 原版 GUI（19 种全覆盖）

```js
GuiJS.openVanilla(player, 'crafting')            // 悬浮工作台（不校验真实方块）
GuiJS.openVanilla(player, 'furnace', [x, y, z])  // 绑定真实熔炉 → 完全功能
GuiJS.openChest(player, '我的箱子', 6)           // 1-6 行箱子
```

名称：`chest` `crafting` `furnace` `blast_furnace` `smoker` `anvil` `smithing` `enchanting_table`
`brewing_stand` `dispenser` `hopper` `shulker_box` `beacon` `lectern` `loom` `stonecutter`
`grindstone` `cartography_table` `merchant`

### 任意模组的 GUI（按菜单类型 ID）

```js
GuiJS.open(player, 'farmersdelight:cooking_pot', block.pos)   // 绑定真实方块 → 完全功能
GuiJS.open(player, 'cgm:attachments')                          // 无上下文菜单
GuiJS.open(player, 'travelersbackpack:backpack_item', buf => { buf.writeInt(2); buf.writeInt(-1) })
GuiJS.listMenus()   // 列出全部已注册菜单（含所有模组）
```

打开机制（三层递进）：

1. **方块绑定**：目标方块实体自带菜单 → 用原厂逻辑打开（等价玩家右键该方块）；
   原版方块实体走标准通道，模组方块实体自动附带 `BlockPos` 载荷。
2. **内置载荷模板**：网络格式特殊的菜单由 `PayloadTemplates` 内置适配
   （`create:schematic_table` / `schematicannon` / `toolbox` 的 pos+NBT、
   `create:stock_keeper_request` 的 bool+bool+pos、`travelersbackpack:backpack_item`）。
3. **通用 buf 工厂**：其余按 `type.create(id, inv, buf)` 构造，脚本可手写 payload；
   打开前预检构造，载荷不匹配时报明确错误。

### Create（6.0.8）兼容情况

| 菜单 | 支持 |
|---|---|
| `create:package_port` / `redstone_requester` / `stock_keeper_category` | ✅ 直接可用（绑定方块坐标） |
| `create:schematic_table` / `schematicannon` / `toolbox` | ✅ 内置 pos+NBT 模板 |
| `create:stock_keeper_request` | ✅ 内置专用模板 |
| `create:crafting_blueprint` | ❌ 依赖图纸实体 |
| `create:filter` / `attribute_filter` / `package_filter` / `linked_controller` / `schedule` | ❌ 手持物品菜单（强校验主手物品） |
| Basin / Millstone / Mixer 等加工机 | 无 GUI（原模组即如此） |

## 二、完全自定义 GUI（脚本定义布局）

### 1. 定义界面（startup_scripts，需重启）

布局是**模块化**的：先设锚点（anchor），再加元素，x/y 是相对锚点位置的偏移 ——
调整 GUI 尺寸时元素自动跟随，不会错位。

```js
GuiJS.createGui('tiny_chest', gui => {
    gui.title('单格箱子')
    gui.size(176, 96)                    // 界面大小（像素，默认 176x166）
    gui.background('kubejs:textures/gui/tiny.png')  // 自定义背景贴图（可选）

    gui.anchor('center', 'top')          // 之后元素：水平居中 + 顶部对齐
    gui.slot(0, 0, 22)                   // x=0 → 正中；y=22 → 距顶部 22px
    gui.grid(0, 9, 3, 0, 17)             // 网格槽位：从容器槽 0 起，9 列 3 行，整体居中
    gui.button('ok', -60, 5, 40, 14, '确定')  // 居中偏左 60px
    gui.label(8, 6, '文本', 0x404040)
    gui.progress('cook', 24, 35, 24, 16, 0)   // 进度条，读 ContainerData[0]（0..10000）
    gui.image('modid:textures/gui/x.png', 10, 10, 24, 16)

    gui.playerInventory()                // 玩家背包：自动底部居中（推荐）
    // 或显式定位：gui.playerInventory(8, 84)
})
```

锚点参数：水平 `'left' | 'center' | 'right'`，垂直 `'top' | 'middle' | 'bottom'`；
恢复绝对定位用 `gui.anchor('left', 'top')`。grid 网格与 playerInventory 作为整块参与锚定。
元素超出面板时启动日志会打印警告（`[GUIJS] Element ... lies outside the GUI panel`）。

不指定背景贴图时使用内置原版风格面板（深色边框 + 高光/阴影）。

### 2. 绑定方块右键打开（JS 实现新箱子/熔炉）

```js
StartupEvents.registry('block', event => {
    event.create('tiny_chest', 'basic')
        .hardness(1.5)
        .blockEntity(info => {
            info.inventory(1, 1)   // KubeJS 内置物品容器附件（1x1 格，自动持久化）
        })
})

GuiJS.bindBlock('kubejs:tiny_chest', 'tiny_chest')   // 右键方块 → 打开 GUI
```

容器槽位直接连接方块实体的 `info.inventory(width, height)` 附件（KubeJS 内置，
物品自动存 NBT）；方块被破坏时 GUI 自动关闭。进度条数据通过方块实体脚本
`data` 标签桥接：tick 里写 `entity.data.guijs0 = 0..10000` 即同步到
`ContainerData[0]`。

### 3. 槽位行为控制（对齐 Java Slot 子类能力）

链式方法作用于**最近添加的槽位**（`gui.slot(...)` 或 `grid` 展开的最后一个槽）：

```js
gui.slot(0, 80, 22)
    .filter('minecraft:iron_ore')              // 只允许放入铁矿石
    .filter(Item.of('diamond_sword').nbt({...})) // IngredientJS 声明 NBT 匹配
    .filter('#forge:ingots/iron')              // 标签过滤
    .filter(stack => stack.id == 'minecraft:iron_ore')  // JS 函数式过滤
    .ghost()                                   // 幽灵槽：点击印入 1 个样本（配置型槽位）
    .locked()                                  // 禁止取出（只进不出）
    .maxStackSize(16)                          // 单格堆叠上限
    .onTake(event => { ... })                  // 取出回调（event.player/stack/slotId/guiId/pos）
    .onChange(event => { ... })                // 槽位内容变化回调
```

### 4. 按钮直接绑定 JS 函数

```js
gui.button('start', 130, 5, 40, 14, '点火', event => {
    event.player.tell('点击了！')            // event.player / buttonId / guiId / pos
})
```

无需再到 server_scripts 写事件分发；全局 `GUIJSEvents.button` 仍会照常触发（两种方式可混用）。

### 5. 动态文本

```js
gui.dataLabel(8, 60, 1, '燃料剩余: ')   // 实时显示 ContainerData[1]（方块实体 tick 里 data.guijs1 = ...）
```

### 6. 按钮点击事件（全局，可选）

```js
GUIJSEvents.button(event => {
    const { player, guiId, buttonId, pos } = event
    if (guiId == 'admin_panel' && buttonId == 'heal') {
        player.health = player.maxHealth
    }
})
```

按钮点击走 vanilla 的 `clickMenuButton` 网络通道，无需自定义协议。

### 4. 手动打开

```js
GuiJS.openCustom(player, 'tiny_chest')        // 无锚点（槽位无持久存储）
GuiJS.openCustom(player, 'tiny_chest', pos)   // 锚定方块实体（槽位绑定其容器）
```

## 三、方块实体（实体方块）能力完全补齐

### GUIJS 自动补齐（无需脚本配置）

| 能力 | 说明 |
|---|---|
| **漏斗 / 管道自动输入输出** | 所有带 `info.inventory(...)` 的 KubeJS 方块实体自动暴露 Forge `IItemHandler` 能力 —— 原版漏斗（Forge 走 capability 路径）与绝大多数模组管道可直接插入/抽取 |
| **输入过滤在管道侧生效** | `info.inventory(w, h, inputFilter)` 的过滤对 GUI 槽位、JS 插入、漏斗/管道三条路径统一生效 |
| **破坏时容器掉落** | 方块被玩家破坏（含创造）或爆炸摧毁时，容器内容像原版箱子一样掉落，不再凭空消失 |

### KubeJS 上游补丁（修改 `其他项目/KubeJS-2001` 源码，GUIJS 示例配套使用）

KubeJS 原生缺失的方块实体红石/生命周期 API 已在上游补齐（common 层，forge/fabric 通用），
需用修改后的 KubeJS 构建 jar 替换 mods 里的 kubejs：

| 新 API（`info.xxx`） | 原生对应 | 说明 |
|---|---|---|
| `neighborChanged((entity, fromPos) => {})` | `Block#neighborChanged` | 邻居方块/红石变化；配合 `level.getBestNeighborSignal(pos)` 读红石输入 |
| `signal((entity, direction) => 15)` | `Block#getSignal` + `isSignalSource` | 作为红石电源输出 0-15 信号（像红石块） |
| `comparatorOutput(entity => 0..15)` | `Block#getAnalogOutputSignal` + `hasAnalogOutputSignal` | 作为比较器输入，按容器/状态输出读数 |
| `onLoad(entity => {})` | `BlockEntity#onLoad` | 方块实体加载（放置/区块加载） |
| `onUnload(entity => {})` | `BlockEntity#onChunkUnloaded` | 区块卸载 |
| `entity.updateNeighbors()` | `Level#updateNeighbourForOutputSignal` | 信号/读数变化后通知邻居刷新（脚本在值变化后调用） |

回调参数 `entity` 为 KubeJS 方块实体：`entity.data`（持久 NBT）、`entity.inventory`（容器）、
`entity.save()` / `entity.sync()`、`entity.x/y/z`。

**补丁改动文件**（KubeJS-2001 内）：
`block/entity/BlockEntityInfo.java`（新回调字段与注册方法）、
`block/entity/BlockEntityJS.java`（`updateNeighbors()`）、
`block/custom/BasicBlockJS.java` 与 `block/custom/HorizontalDirectionalBlockBuilder.java`
（`WithEntity` 方块类接入红石/比较器原链路）、
`forge/.../core/mixin/forge/BlockEntityMixin.java`（Forge 生命周期钩子 onLoad/onChunkUnloaded
分派到脚本回调，已注册进 `kubejs-forge.mixins.json`）。

**使用方式**：构建修改版 KubeJS：`cd 其他项目/KubeJS-2001 && gradlew :forge:build`，
用 `forge/build/libs/kubejs-forge-2001.6.5-local.*.jar` 替换 mods 中的 kubejs
（gradle wrapper 的发行版下载地址已改为腾讯镜像）。

完整演示脚本：`kubejs_examples/startup_scripts/guijs_full_demo.js` 与
`kubejs_examples/server_scripts/guijs_full_demo.js`。

## 四、事件与查询

```js
GUIJSEvents.opened(event => { ... })   // event.player / menuId / pos
GUIJSEvents.button(event => { ... })   // event.player / guiId / buttonId / pos
GuiJS.listMenus() / findMenus('关键字') / hasMenu('modid:path')
```

## 已知限制

- 纯 Screen 界面（无容器菜单的模组，如 Heracles / Questlog / Field-Guide）不走
  `MenuType` 体系，服务端无法通用打开，需逐个接模组自有网络包。
- 手持物品型菜单（Create 的 filter 系）依赖主手物品校验，不适用通用打开。
- 自定义 GUI 的 `label` 目前为静态文本；动态内容用 `progress`（数值）表达。
- 自定义 GUI 未使用自定义背景贴图时，使用内置简约面板样式。

## 目录

- `src/main/java/com/guijs/gui/` — 打开引擎（GuiOpener / VanillaGuis / PayloadTemplates / OpenAccess）
- `src/main/java/com/guijs/gui/custom/` — 自定义 GUI 框架（描述模型 / 构建器 / 注册表 / 菜单）
- `src/main/java/com/guijs/client/` — 客户端渲染（GuiJSScreen / MenuScreens 注册）
- `src/main/java/com/guijs/kubejs/` — KubeJS 插件、绑定、事件
- `kubejs_examples/` — 脚本示例（自定义 GUI 定义 + 打开/事件）
