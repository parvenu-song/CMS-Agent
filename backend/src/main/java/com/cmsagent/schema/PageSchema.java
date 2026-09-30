package com.cmsagent.schema;

import java.util.*;

/** Framework-independent executable contract. Keep constraints aligned with packages/contracts. */
public record PageSchema(int version, String code, String title, List<Field> fields) {
    private static final Set<String> RESERVED = Set.of("constructor", "prototype", "__proto__", "id", "version", "createdAt", "updatedAt");
    private static final Set<String> TYPES = Set.of("text", "textarea", "number", "boolean", "select");
    public PageSchema {
        require(version == 1, "仅支持 Schema version 1");
        require(code != null && code.matches("[a-z][a-z0-9]{1,31}"), "模块标识格式错误");
        text(title, 80);
        require(fields != null && !fields.isEmpty() && fields.size() <= 30, "字段数量必须为 1–30");
        var names = new HashSet<String>();
        for (Field f : fields) require(f != null && names.add(f.name()), "字段为空或重名");
        require(fields.stream().anyMatch(Field::list), "至少一个列表字段");
        fields = List.copyOf(fields);
    }
    public record Field(String name, String label, String type, Boolean required, Boolean list, List<String> options) {
        public Field {
            require(name != null && name.matches("[a-z][a-zA-Z0-9]{0,31}") && !RESERVED.contains(name), "非法或保留字段名");
            text(label, 80);
            require(type != null && TYPES.contains(type), "字段类型无效");
            require(required != null && list != null, "required / list 必须明确指定");
            if ("select".equals(type)) {
                require(options != null && !options.isEmpty() && options.size() <= 20, "select 选项数量必须为 1–20");
                options.forEach(v -> text(v, 80));
                require(new HashSet<>(options).size() == options.size(), "选项重复");
                options = List.copyOf(options);
            } else require(options == null, "只有 select 支持 options");
        }
    }
    public Map<String, Object> validateData(Map<String, Object> raw) {
        require(raw != null, "缺少 data");
        var allowed = new HashSet<String>();
        fields.forEach(f -> allowed.add(f.name()));
        require(allowed.containsAll(raw.keySet()), "存在未知字段");
        var result = new LinkedHashMap<String, Object>();
        for (Field f : fields) {
            Object value = raw.get(f.name());
            if (value == null || value instanceof String s && s.isBlank()) {
                require(!f.required(), f.label() + "不能为空");
                result.put(f.name(), null); continue;
            }
            switch (f.type()) {
                case "boolean" -> require(value instanceof Boolean, f.label() + "必须为布尔值");
                case "number" -> require(value instanceof Number n && Double.isFinite(n.doubleValue()) && Math.abs(n.doubleValue()) <= 1e12, f.label() + "数字无效或超出范围");
                default -> {
                    require(value instanceof String s && s.length() <= (f.type().equals("textarea") ? 20000 : 200), f.label() + "类型错误或过长");
                    if (f.type().equals("select")) require(f.options().contains(value), f.label() + "选项无效");
                }
            }
            result.put(f.name(), value);
        }
        return result;
    }
    private static void text(String v, int max) { require(v != null && !v.isBlank() && v.length() <= max, "文本不能为空或过长"); }
    public static void require(boolean valid, String message) { if (!valid) throw new IllegalArgumentException(message); }
}
