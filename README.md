# 项目说明
该项目的目标是尽可能多的将高性能次世代的图形学技术做一个集合，方便图形专业人士的学习和交流使用，<b>目前还处于开发阶段</b>。
计划将来做成稳定的工具库，便于集成到其它的游戏引擎中（例如 LibGDX, Jmonkey Enginner等）。
已完成的Demo都已在Nvidia显卡GTX960和GTX970上通过测试。<br>
+ 目前包含三个模块
    - Post Processing(后期处理)
    - Shadow Library (阴影)
    - Water Effect (水效果)
+ 目前支持LWJGL和JOGL绑定，将来可能会支持Android平台
+ 运行方法：下载Android Studio，导入项目即可

# Post Processing模块
**该模块的基础架构仿虚幻引擎而来，大多数功能都已经稳定，功能如下：**
## Features
+ SSAO
  - HBAO
  - ASSAO

+ Atmospheric Scattering
  - Support the Cascade Shadow Map
  - Support the Direction Lighting model

+ Volumetric Lighting
  - Support the Point and Direction Lighting model

+ Depth of Field
  - DOF Gaussion
  - DOF Bokeh

+ Tone mapping

+ HDR
  - Lens flare
  - Light Streaker

+ AntiAliasing
  - FXAA
  - SMAA
  - SAA
+ Clouding(Still in progress...)

# Shadow Library
**正在开发中**

# Water Effect
**正在开发中**

# Samples ScreenShot
## Atmospheric Scattering
![index](https://github.com/mzhg/PostProcessingWork/blob/master/screenshot/OutDoor.jpg)
## Volumetric Lighting
![index](https://github.com/mzhg/PostProcessingWork/blob/master/screenshot/VolumetricLighting.jpg)
## SSAO
![index](https://github.com/mzhg/PostProcessingWork/blob/master/screenshot/SSAO.jpg)
## Depth of Field
![index](https://github.com/mzhg/PostProcessingWork/blob/master/screenshot/DOF.jpg)
## HDR
![index](https://github.com/mzhg/PostProcessingWork/blob/master/screenshot/HDR.jpg)
## AntiAliasing
![index](https://github.com/mzhg/PostProcessingWork/blob/master/screenshot/AntiAliasing.jpg)
## Water Waves
![index](https://github.com/mzhg/PostProcessingWork/blob/master/screenshot/WaterWave.jpg)
## Other Intersting Demos
![index](https://github.com/mzhg/PostProcessingWork/blob/master/screenshot/Other.jpg)

# License agreement

See the Apache 2.0 license.txt for full license agreement details.