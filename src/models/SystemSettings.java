package models;

public class SystemSettings {
    private int settingId;
    private String keyName;
    private String keyValue;

    public SystemSettings() {}

    public SystemSettings(int settingId, String keyName, String keyValue) {
        this.settingId = settingId;
        this.keyName = keyName;
        this.keyValue = keyValue;
    }

    // Getters & Setters
    public int getSettingId() { return settingId; }
    public void setSettingId(int settingId) { this.settingId = settingId; }

    public String getKeyName() { return keyName; }
    public void setKeyName(String keyName) { this.keyName = keyName; }

    public String getKeyValue() { return keyValue; }
    public void setKeyValue(String keyValue) { this.keyValue = keyValue; }
}
