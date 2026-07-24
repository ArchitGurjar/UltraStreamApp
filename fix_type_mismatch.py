import os

path = "app/src/main/java/com/ultrastream/app/utils/DebridHelper.kt"

# फाइल को रीड करना
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

# 1. Network के सारे Imports एक साथ ऐड करना
if "import com.ultrastream.app.network.*" not in code:
    code = code.replace("import com.ultrastream.app.network.RealDebridApi", 
                        "import com.ultrastream.app.network.*\nimport com.ultrastream.app.network.RealDebridApi")

# 2. सिर्फ फाइल के नीचे वाले डुप्लीकेट Data Classes को काटना
marker = "// =========================== DATA CLASSES FOR APIS ==========================="
if marker in code:
    # मार्कर से पहले का सारा असली कोड रख लेना और बाकी हटा देना
    code = code.split(marker)[0].strip() + "\n"
    
    with open(path, "w", encoding="utf-8") as f:
        f.write(code)
    print("✅ DebridHelper.kt SAFELY PATCHED! Duplicate data classes removed without minifying.")
else:
    # अगर मार्कर नहीं मिला तो फाइल को वैसे ही सेव कर देना
    with open(path, "w", encoding="utf-8") as f:
        f.write(code)
    print("✅ No duplicate classes found to remove.")

