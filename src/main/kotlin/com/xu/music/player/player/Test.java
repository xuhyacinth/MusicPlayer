package com.xu.music.player.player;

public class Test {


    public Test() {
        // 创建一个SimpleUniverse对象
        SimpleUniverse universe = new SimpleUniverse();

        // 创建一个BranchGroup对象
        BranchGroup group = new BranchGroup();

        // 创建一个ColorCube对象（一个简单的立方体）
        ColorCube cube = new ColorCube(0.3);

        // 将立方体添加到BranchGroup中
        group.addChild(cube);

        // 将BranchGroup添加到SimpleUniverse中
        universe.getViewingPlatform().setNominalViewingTransform();
        universe.addBranchGraph(group);
    }

    public static void main(String[] args) {
        // 创建一个Frame对象来显示3D场景
        new MainFrame(new Simple3D(), 800, 600);
    }

}
