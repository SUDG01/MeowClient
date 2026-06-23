package cn.sux1ng.client.config.configs;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.config.Config;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import cn.sux1ng.client.value.TextValue;
import cn.sux1ng.client.value.Value;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement; // 导入这个
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map; // 导入这个

public class ModConfig extends Config {

    public ModConfig() {
        super("Mod");
    }

    @Override
    public void load() {
        try {
            // 防空文件处理
            if (!getPath().toFile().exists()) return;
            byte[] bytes = Files.readAllBytes(getPath());
            if (bytes.length == 0) return;

            JsonObject jsonObject = new Gson().fromJson(new String(bytes, StandardCharsets.UTF_8), JsonObject.class);
            if (jsonObject == null) return;

            for (Mod mod : MeowClient.modManager.getMods()) {
                if (jsonObject.has(mod.getName())) {
                    JsonObject modJson = jsonObject.get(mod.getName()).getAsJsonObject();

                    // 1. 读取开关状态
                    if (modJson.has("enable")) {
                        boolean targetState = modJson.get("enable").getAsBoolean();
                        if (mod.isEnable() != targetState) {
                            mod.setEnable(targetState);
                        }
                    }

                    // 2. 读取按键绑定
                    if (modJson.has("key")) {
                        mod.setKey(modJson.get("key").getAsInt());
                    }

                    // 3. 【新增】读取参数设置 (Values)
                    if (modJson.has("values")) {
                        JsonObject valuesJson = modJson.get("values").getAsJsonObject();

                        // 遍历模块里现有的所有参数
                        for (Value<?> value : mod.getValues()) {
                            // 如果配置文件里有这个参数的名字
                            if (valuesJson.has(value.getName())) {
                                try {
                                    JsonElement element = valuesJson.get(value.getName());

                                    // 根据类型分别恢复数据
                                    if (value instanceof BooleanValue) {
                                        ((BooleanValue) value).setValue(element.getAsBoolean());
                                    } else if (value instanceof NumberValue) {
                                        ((NumberValue) value).setValue(element.getAsDouble());
                                    } else if (value instanceof ModeValue) {
                                        ((ModeValue) value).setValue(element.getAsString());
                                    } else if (value instanceof ColorValue) {
                                        // ColorValue 保存为 JSON 对象 {rgb, hue, saturation, brightness, alpha}
                                        if (element.isJsonObject()) {
                                            com.google.gson.JsonObject colorObj = element.getAsJsonObject();
                                            ColorValue cv = (ColorValue) value;
                                            if (colorObj.has("hue")) {
                                                cv.setHue(colorObj.get("hue").getAsFloat());
                                                cv.setSaturation(colorObj.get("saturation").getAsFloat());
                                                cv.setBrightness(colorObj.get("brightness").getAsFloat());
                                                cv.setAlpha(colorObj.get("alpha").getAsInt());
                                            } else if (colorObj.has("rgb")) {
                                                // 兼容旧格式：只有 rgb int
                                                cv.setValue(colorObj.get("rgb").getAsInt());
                                            }
                                        } else {
                                            // 兼容纯 int 格式
                                            ((ColorValue) value).setValue(element.getAsInt());
                                        }
                                    } else if (value instanceof TextValue) {
                                        ((TextValue) value).setValue(element.getAsString());
                                    }
                                } catch (Exception e) {
                                    System.out.println("Error loading value: " + value.getName());
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Config load failed: " + e.getMessage());
        }
    }

    @Override
    public void save() {
        JsonObject jsonObject = new JsonObject();

        for (Mod mod : MeowClient.modManager.getMods()) {
            JsonObject modJson = new JsonObject();

            // 1. 保存基础信息
            modJson.addProperty("enable", mod.isEnable());
            modJson.addProperty("key", mod.getKey());

            // 2. 【新增】保存参数设置
            if (!mod.getValues().isEmpty()) {
                JsonObject valuesJson = new JsonObject();

                for (Value<?> value : mod.getValues()) {
                    // 根据类型保存
                    if (value instanceof BooleanValue) {
                        valuesJson.addProperty(value.getName(), ((BooleanValue) value).getValue());
                    } else if (value instanceof NumberValue) {
                        valuesJson.addProperty(value.getName(), ((NumberValue) value).getValue());
                    } else if (value instanceof ModeValue) {
                        valuesJson.addProperty(value.getName(), ((ModeValue) value).getValue());
                    } else if (value instanceof ColorValue) {
                        // ColorValue 保存为包含 HSB 信息的 JSON 对象
                        ColorValue cv = (ColorValue) value;
                        com.google.gson.JsonObject colorObj = new com.google.gson.JsonObject();
                        colorObj.addProperty("rgb", cv.getValue());
                        colorObj.addProperty("hue", cv.getHue());
                        colorObj.addProperty("saturation", cv.getSaturation());
                        colorObj.addProperty("brightness", cv.getBrightness());
                        colorObj.addProperty("alpha", cv.getAlpha());
                        valuesJson.add(value.getName(), colorObj);
                    } else if (value instanceof TextValue) {
                        valuesJson.addProperty(value.getName(), ((TextValue) value).getValue());
                    }
                }

                // 把参数包放进模块的 Json 里
                modJson.add("values", valuesJson);
            }

            jsonObject.add(mod.getName(), modJson);
        }

        try {
            Files.write(getPath(), new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}