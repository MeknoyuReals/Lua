package com.example.lua

import java.util.UUID
import kotlin.random.Random

object LuaEngine {

    // Common Roblox/Lua globals to exclude from obfuscation
    private val EXCLUDED_GLOBALS = setOf(
        "and", "break", "do", "else", "elseif", "end", "false", "for", "function", "if",
        "in", "local", "nil", "not", "or", "repeat", "return", "then", "true", "until", "while",
        "game", "workspace", "script", "task", "print", "warn", "error", "Instance", "Vector3",
        "CFrame", "Color3", "TweenInfo", "Enum", "shared", "_G", "string", "table", "math",
        "pairs", "ipairs", "next", "pcall", "xpcall", "getgenv", "fireclickdetector", "hookmetamethod",
        "setreadonly", "getrawmetatable", "Drawing", "setclipboard", "loadstring", "os", "debug",
        "coroutine", "utf8", "delay", "spawn", "wait", "tick", "assert", "select", "type",
        "tostring", "tonumber", "rawget", "rawset", "rawequal", "setmetatable", "getmetatable", "require"
    )

    /**
     * Remove comments from Lua code.
     */
    fun removeComments(code: String): String {
        // Remove block comments: --[[ ... ]]
        var cleaned = code.replace(Regex("--\\[\\[[\\s\\S]*?\\]\\]"), "")
        
        // Remove single line comments: -- ...
        // We do this line by line to avoid stripping double dashes inside string literals
        val lines = cleaned.split("\n")
        val processedLines = lines.map { line ->
            var inString = false
            var stringChar: Char? = null
            var commentIndex = -1
            var i = 0
            while (i < line.length) {
                val c = line[i]
                if ((c == '"' || c == '\'') && (i == 0 || line[i - 1] != '\\')) {
                    if (!inString) {
                        inString = true
                        stringChar = c
                    } else if (stringChar == c) {
                        inString = false
                        stringChar = null
                    }
                }
                if (!inString && c == '-' && i + 1 < line.length && line[i + 1] == '-') {
                    commentIndex = i
                    break
                }
                i++
            }
            if (commentIndex != -1) {
                line.substring(0, commentIndex).trimEnd()
            } else {
                line
            }
        }
        return processedLines.filter { it.isNotBlank() }.joinToString("\n")
    }

    /**
     * Obfuscate string literals by converting them into dynamic byte-decryption tables.
     * e.g., "hello" -> (function() local b={104,101,108,108,111} local s="" for i=1,#b do s=s..string.char(b[i]) end return s end)()
     */
    fun encryptStrings(code: String): String {
        val stringRegex = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"|'((?:[^'\\\\]|\\\\.)*)'|\\s*\\[\\[([\\s\\S]*?)\\]\\]")
        
        return stringRegex.replace(code) { match ->
            val originalStr = match.groupValues[1].ifEmpty { 
                match.groupValues[2].ifEmpty { 
                    match.groupValues[3] 
                } 
            }
            
            // If empty or very short, keep it
            if (originalStr.isEmpty()) return@replace match.value
            
            // Convert to byte values
            val bytes = originalStr.map { it.code }.joinToString(",")
            
            // Generate decryption snippet
            // Lua allows inline self-executing functions
            "(function()local b={$bytes}local s=\"\"for i=1,#b do s=s..string.char(b[i])end return s end)()"
        }
    }

    /**
     * Rename local variables and functions to random hexadecimal identifiers.
     */
    fun renameVariables(code: String): String {
        val localVars = mutableSetOf<String>()
        
        // Find local function declarations: local function abc(...)
        val localFnRegex = Regex("local\\s+function\\s+([a-zA-Z_][a-zA-Z0-9_]*)")
        localFnRegex.findAll(code).forEach {
            val name = it.groupValues[1]
            if (name !in EXCLUDED_GLOBALS) {
                localVars.add(name)
            }
        }
        
        // Find local variable declarations: local abc = ... or local abc, def = ...
        // We match "local" followed by identifiers up to the '=' or end of statement
        val localDeclRegex = Regex("local\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\s*,\\s*[a-zA-Z_][a-zA-Z0-9_]*)*)\\s*(?:=|$)")
        localDeclRegex.findAll(code).forEach { match ->
            val variablesList = match.groupValues[1]
            variablesList.split(",").forEach { variable ->
                val trimmed = variable.trim()
                if (trimmed.isNotEmpty() && trimmed !in EXCLUDED_GLOBALS) {
                    localVars.add(trimmed)
                }
            }
        }

        if (localVars.isEmpty()) return code

        // Create random mapping
        val mapping = mutableMapOf<String, String>()
        localVars.forEach { original ->
            // Format: _mek_0x[4 digit random hex]
            val randomHex = String.format("%04x", Random.nextInt(0, 65535))
            val newName = "_mek_0x$randomHex"
            mapping[original] = newName
        }

        // Sort by length descending to avoid replacing partial names first
        val sortedVars = localVars.sortedByDescending { it.length }

        var obfuscatedCode = code
        for (variable in sortedVars) {
            val replacement = mapping[variable] ?: continue
            // Use word boundary to replace exact variable name only
            obfuscatedCode = obfuscatedCode.replace(Regex("\\b$variable\\b"), replacement)
        }

        return obfuscatedCode
    }

    /**
     * Injects harmless junk statements into the Lua code.
     */
    fun injectJunk(code: String): String {
        val lines = code.split("\n")
        val result = mutableListOf<String>()
        
        val junkTemplates = listOf(
            { "local _mek_junk_${String.format("%04x", Random.nextInt(0, 65535))} = ${Random.nextInt(100, 999)} + ${Random.nextInt(100, 999)}" },
            { "if false then print(\"meknoyu bypass check\") end" },
            { "local _mek_math_${String.format("%04x", Random.nextInt(0, 65535))} = math.sin(${Random.nextDouble(0.0, 1.0)})" },
            { "local _mek_lib_${String.format("%04x", Random.nextInt(0, 65535))} = function() return \"meknoyu\" end" }
        )

        for (line in lines) {
            result.add(line)
            // Inject random junk statement with a 20% probability on non-empty, non-keyword lines
            if (line.trim().isNotEmpty() && !line.trim().startsWith("end") && !line.trim().startsWith("else") && Random.nextInt(0, 100) < 20) {
                val junk = junkTemplates.random().invoke()
                result.add(junk)
            }
        }
        
        return result.joinToString("\n")
    }

    /**
     * Complete Obfuscation Pipeline.
     */
    fun obfuscate(
        code: String,
        removeComments: Boolean,
        encryptStrings: Boolean,
        renameVariables: Boolean,
        injectJunk: Boolean,
        minify: Boolean
    ): String {
        var processed = code
        
        if (removeComments) {
            processed = removeComments(processed)
        }
        if (injectJunk) {
            processed = injectJunk(processed)
        }
        if (renameVariables) {
            processed = renameVariables(processed)
        }
        if (encryptStrings) {
            processed = encryptStrings(processed)
        }
        if (minify) {
            // Minify: compress spacing and join lines where possible
            processed = processed.replace(Regex("\\s+"), " ")
                .replace(Regex("\\s*([\\+\\-\\*/%\\^#=<>;,\\(\\)\\{\\}\\[\\]])\\s*"), "$1")
                .trim()
        } else {
            // Beautify/re-format slightly
            processed = beautify(processed)
        }

        return processed
    }

    /**
     * Simple Lua Beautifier.
     */
    fun beautify(code: String): String {
        val cleaned = code.trim()
        if (cleaned.isEmpty()) return ""

        val lines = cleaned.split("\n")
        val result = StringBuilder()
        var indentLevel = 0
        
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) {
                result.append("\n")
                continue
            }

            // Detect matching end statements to decrement before writing line
            var decBefore = false
            if (line.startsWith("end") || line.startsWith("else") || line.startsWith("elseif") || line.startsWith("until")) {
                indentLevel = maxOf(0, indentLevel - 1)
                decBefore = true
            }

            // Write indents
            for (i in 0 until indentLevel) {
                result.append("    ")
            }
            result.append(line).append("\n")

            // If we already decremented, we don't double count. Otherwise, we check what's next
            if (decBefore) {
                // If it was "else" or "elseif", we need to increment back for the next line
                if (line.startsWith("else") || line.startsWith("elseif")) {
                    indentLevel++
                }
            } else {
                // Check if the line opens a block
                val opensBlock = line.startsWith("if ") || 
                               line.startsWith("function ") || 
                               line.startsWith("local function ") ||
                               line.startsWith("for ") || 
                               line.startsWith("while ") || 
                               line.contains(" then") || 
                               line.endsWith(" do") || 
                               line.endsWith("repeat") ||
                               line.endsWith("{")
                
                if (opensBlock && !line.endsWith("end")) {
                    indentLevel++
                }
            }
        }

        return result.toString().trim()
    }

    /**
     * Regex/Pattern-based Lua Deobfuscator.
     * Decodes custom encrypted strings and character escape sequences.
     */
    fun deobfuscatePattern(code: String): String {
        var processed = code

        // 1. Decode character code sequences: string.char(104, 101, 108, 108, 111)
        val stringCharRegex = Regex("string\\.char\\s*\\(\\s*([0-9\\s*,]+)\\s*\\)")
        processed = stringCharRegex.replace(processed) { match ->
            val numbersStr = match.groupValues[1]
            val decoded = numbersStr.split(",")
                .map { it.trim().toIntOrNull() ?: 0 }
                .map { it.toChar() }
                .joinToString("")
            "\"$decoded\""
        }

        // 2. Decode custom dynamic encryption:
        // (function()local b={104,101,108,108,111}local s=""for i=1,#b do s=s..string.char(b[i])end return s end)()
        val customDecryptRegex = Regex("\\(function\\(\\)\\s*local\\s+b\\s*=\\s*\\{([0-9\\s*,]+)\\}\\s*local\\s+s\\s*=\\s*\"\"\\s*for\\s+i\\s*=\\s*1\\s*,\\s*#b\\s+do\\s+s\\s*=\\s*s\\s*\\.\\.\\s*string\\.char\\(b\\[i\\]\\)\\s*end\\s*return\\s+s\\s+end\\s*\\)\\s*\\(\\s*\\)")
        processed = customDecryptRegex.replace(processed) { match ->
            val numbersStr = match.groupValues[1]
            val decoded = numbersStr.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .map { it.toChar() }
                .joinToString("")
            "\"$decoded\""
        }

        // 3. Decode standard string escape sequences: \104\101\108\108\111
        val decimalEscapeRegex = Regex("\\\\([0-9]{3})")
        processed = decimalEscapeRegex.replace(processed) { match ->
            val codePoint = match.groupValues[1].toIntOrNull() ?: return@replace match.value
            codePoint.toChar().toString()
        }

        // 4. Decode hex escape sequences: \x68\x65\x6c\x6c\x6f
        val hexEscapeRegex = Regex("\\\\x([0-9a-fA-F]{2})")
        processed = hexEscapeRegex.replace(processed) { match ->
            val codePoint = match.groupValues[1].toInt(16)
            codePoint.toChar().toString()
        }

        // Beautify the output
        return beautify(processed)
    }
}
