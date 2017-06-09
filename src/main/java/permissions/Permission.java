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
                permissionList.add(Value.ALL);
                break;
            case ALL:
                permissionList.add(Value.ALL);
                break;
        }

        return permissionList;
    }

    private int getTopLevelPermissionValue() {
        if (permissionList.contains(Value.MEOWERS)) {
            return Value.MEOWERS.getValue();
        } else {
            return Value.ALL.getValue();
        }
    }

    public boolean equalsOrGreater(Permission permission) {
        return (this.getTopLevelPermissionValue() >= permission.getTopLevelPermissionValue());
    }
}
