# 敌人位置标记原型（Unreal Engine）

这套脚本用于你自己的游戏工程，演示「敌人什么时候该被看见、位置怎么标到屏幕上」。逻辑拆成两块，方便你之后按需移植或替换：

- `Source/EnemyRadar/EnemyRadarComponent.h/.cpp`：侦测与记忆，挂在玩家角色上。
- `Source/EnemyRadar/EnemyRadarHUD.h/.cpp`：屏幕标记绘制，红色菱形 / 蓝色声源圆圈 / 屏幕外箭头 / 小地图。

## 运行效果

- 敌人在视野半角 55 度（总视角 110 度）、3200 cm 内、且视线中没有墙时，显示红色菱形标记，带名字和距离。
- 敌人移动且进入 900 cm 听觉范围时，在最后听到的位置显示蓝色圆圈「声源」。
- 敌人离开视野后，标记保留 4 秒并逐渐淡出，不会立刻消失。
- 目标在屏幕外时，画面边缘出现指向箭头和距离。
- 右下角小地图显示已经发现过的目标，随相机朝向旋转。

## 接入步骤

1. 把 `Source/EnemyRadar` 整个文件夹复制到你工程的 `Source/<你的工程名>/EnemyRadar/` 下。
2. 打开敌人蓝图，Class Defaults → Actor → Tags 里加一个 `Enemy` 标签（要和组件里的 `EnemyActorTag` 一致）。
3. 打开玩家角色蓝图，Add Component 搜索 `Enemy Radar` 添加。组件会自动找角色上的第一个 Camera Component 作为视野原点。
4. 新建蓝图类，父类选 `EnemyRadarHUD`，命名为 `BP_EnemyRadarHUD`；项目设置 → Maps & Modes → HUD Class 选它。已经有自定义 HUD 的话，把 `DrawHUD` 里的绘制代码搬进你的 HUD 即可，不必替换。
5. 编译并运行（PIE 即可）。

工程需要是 C++ 工程；纯蓝图工程在添加 C++ 类时编辑器会提示转换，转换后能正常编译。

## 可调参数

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `EnemyActorTag` | `Enemy` | 敌人 Actor 的标签；不用 Tag 时改用 `SetTargets` |
| `SightRange` | 3200 | 视野距离，单位 cm |
| `SightHalfAngle` | 55 | 视野半角，总视角是两倍 |
| `HearingRadius` | 900 | 听声半径，单位 cm |
| `MinNoiseSpeed` | 30 | 低于这个速度视为静止，不触发听声 |
| `MemoryDuration` | 4 | 离开视野后标记保留的秒数 |
| `bRequireLineOfSight` | true | 是否要求视线无遮挡 |
| `SightTraceChannel` | Visibility | 遮挡检测通道，墙体需要挡住它 |
| `bUseCameraForSight` | true | 用相机位置和朝向判定；关闭则用角色 |
| `MarkerHeight` | 90 | 标记和视线采样点的高度偏移 |
| `MarkerSize` / `SightColor` / `SoundColor` | 11 / 红 / 蓝 | HUD 标记外观 |
| `MinimapWorldRadius` | 3500 | 小地图半径覆盖的世界距离 |

不想依赖 Tag 时，可以在敌人生成或关卡开始时手动登记：

```cpp
RadarComponent->SetTargets(AllEnemyActors);
```

## 看不到标记时先查这几项

1. 敌人 Actor 的 Tags 里是否有 `Enemy`，拼写大小写要一致。
2. 墙壁是否在 `Visibility` 通道上设为 Block。默认用 Visibility 做遮挡检测，墙不挡就会穿墙显示。
3. 相机是不是玩家 Pawn 上的 `UCameraComponent`。如果相机挂在别处，把 `bUseCameraForSight` 关掉，用角色位置判定。
4. HUD 是否真的切到了 `BP_EnemyRadarHUD`；这一步没做时组件有数据但画不出来。
5. 打开组件的 `bDrawDebug`，看有没有红线/蓝线，能区分是侦测问题还是绘制问题。

## 移植到其他引擎时保留的思路

核心只有四步，和引擎 API 无关：

1. 每隔一小段时间遍历候选敌人，算距离和水平夹角；
2. 从眼睛位置向敌人做一次射线检测，确认中间没有墙；
3. 通过判定后记录「最后已知位置 + 时间戳」，之后每帧按时间差做渐隐；
4. 把最后已知位置加上高度偏移，投影到屏幕坐标；投影失败或超出屏幕时，用相机空间方向算边缘箭头。

换成 Unity 就是 `Vector3.Distance` + `Vector3.Dot` + `Physics.Linecast` + `Camera.WorldToScreenPoint`；换成 Godot 就是 `RayCast3D` + `Camera3D.UnprojectPosition`。判定顺序和记忆时间的做法可以直接照搬。

注意：本机没有安装 Unreal，这套代码按 UE5 常用 API 编写、兼容 UE4.27 的主要接口，但未在编辑器里实际编译过。接入后先编译一次，再按上面的参数调手感。
