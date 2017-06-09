package permissions;

import java.util.ArrayList;
import java.util.List;

public class Permission {
    public enum Value {
        MEOWERS(1),
        ALL(0);

        int value;
        Value(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private List<Value> permissionList;

    public Permission(Value permission) {
        this.permissionList = addHierachyOfPermission(permission);
    }

    private List<Value> addHierachyOfPermission(Value permission) {
        permissionList = new ArrayList<>();

        switch (permission) {
            case MEOWERS:
                permissionList.add(Value.MEOWERS);
            case ALL:
                permissionList.add(Value.ALL);
                break;
        }

        return permissionList;
    }

    private int getTopLevelPermissionValue() {
        int highestValue = 0;
        for (Value val : permissionList) {
            int getVal = val.getValue();
            if (getVal > highestValue) {
                highestValue = getVal;
            }
        }
        return highestValue;
    }

    public boolean equalsOrGreater(Permission permission) {
        return (this.getTopLevelPermissionValue() >= permission.getTopLevelPermissionValue());
    }
}
