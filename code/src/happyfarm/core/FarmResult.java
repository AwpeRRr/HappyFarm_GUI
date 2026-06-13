package happyfarm.core;

import happyfarm.model.FarmObject;

public class FarmResult {
    private final boolean success;
    private final String message;
    private final FarmObject target;

    private FarmResult(boolean success, String message, FarmObject target) {
        this.success = success;
        this.message = message;
        this.target = target;
    }

    public static FarmResult success(String message) {
        return new FarmResult(true, message, null);
    }

    public static FarmResult success(String message, FarmObject target) {
        return new FarmResult(true, message, target);
    }

    public static FarmResult fail(String message) {
        return new FarmResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public FarmObject getTarget() {
        return target;
    }
}
