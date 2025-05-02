package org.example.common.response;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class CommandResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 5L;
    private final boolean success;
    private final String message;
    private final Object resultData;

    public CommandResponse(boolean success, String message, Object resultData) {
        this.success = success;
        this.message = message;
        this.resultData = resultData;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getResultData() { return resultData; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (message != null && !message.isEmpty()) {
            sb.append(message);
            if (resultData != null) sb.append("\n");
        }
        if (resultData != null) {
            if (resultData instanceof List) {
                ((List<?>) resultData).forEach(item -> sb.append(item).append("\n"));
                if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '\n') {
                    sb.setLength(sb.length() - 1);
                }
            } else {
                sb.append(resultData);
            }
        }
        return sb.toString();
    }
}