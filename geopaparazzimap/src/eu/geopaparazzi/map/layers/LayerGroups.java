package eu.geopaparazzi.map.layers;

@SuppressWarnings("ALL")
public enum LayerGroups {

    GROUP_USERLAYERS(0, "userlayers"),//
    GROUP_SYSTEM(1, "systemlayers"),//
    GROUP_3D(2, "3dlayers"),//
    GROUP_SYSTEM_TOP(3, "systemtoplayers");

    private final int groupId;
    private final String groupName;

    LayerGroups(int groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
    }

    public int getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }
}
