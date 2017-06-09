package permissions;

public class Permission {
    public enum Value {
        MEOWERS,
        ALL;
    }

    private Permission.Value permission;

    public Permission(Permission.Value permission) {
        this.permission = permission;
    }

    public Permission.Value getValue() {
        return permission;
    }
}
