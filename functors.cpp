#include <iostream>
#include <string>
#include <sstream>
#include <iomanip>
#include <cstring>
#include <limits>
#include <regex>
#include <cctype>
#include <cstdlib>

static char* copy_to_cstr(const std::string& s) {
    char* result = (char*)std::malloc(s.size() + 1);
    if (!result) return nullptr;
    std::strcpy(result, s.c_str());
    return result;
}

static std::string to_string_safe(const char* input) {
    return input == nullptr ? std::string() : std::string(input);
}

static bool starts_with_impl(const std::string& s, const std::string& prefix) {
    return s.size() >= prefix.size() && s.rfind(prefix, 0) == 0;
}

static bool ends_with_impl(const std::string& s, const std::string& suffix) {
    return s.size() >= suffix.size() &&
           s.compare(s.size() - suffix.size(), suffix.size(), suffix) == 0;
}

static bool is_hex_digit(char c) {
    return std::isxdigit(static_cast<unsigned char>(c)) != 0;
}

// Extern "C" block to allow linkage with C code
extern "C" {

// Function to convert a C string (char*) to an IRI
char* toIRI(const char* input) {
    // Convert the input C string to a C++ string
    std::string str(input);
    std::ostringstream iriStream;

    for (char c : str) {
        // Percent-encode characters that are not allowed in IRIs
        if (isalnum(c) || c == '-' || c == '_' || c == '.' || c == '~') {
            iriStream << c;
        } else {
            // Convert the character to a percent-encoded string
            iriStream << '%' << std::uppercase << std::hex << std::setw(2) << std::setfill('0') << static_cast<int>(static_cast<unsigned char>(c));
        }
    }

    // Convert the result back to a C string (char*)
    std::string iriStr = iriStream.str();
    char* result = (char*)malloc(iriStr.length() + 1); // Allocate memory for the C string
    std::strcpy(result, iriStr.c_str()); // Copy the C++ string to the allocated C string

    return result;
}

char* toDoubleLiteral(const char* input) {
    if (!input) return NULL;

    // Convert input to a double
    double number;
    std::istringstream iss(input);
    iss >> number;
    if (iss.fail()) return NULL;

    // Create an output string stream
    std::ostringstream oss;

    // Format the number in scientific notation with uppercase
    oss << std::uppercase << std::scientific << number;

    // Get the formatted string
    std::string result = oss.str();

    // Find the position of the exponent ('E')
    size_t ePos = result.find('E');
    if (ePos != std::string::npos) {
        // Remove unnecessary trailing zeros in the mantissa part
        size_t dotPos = result.find('.');
        if (dotPos != std::string::npos && dotPos < ePos) {
            size_t lastNonZero = result.find_last_not_of('0', ePos - 1);
            if (lastNonZero != std::string::npos) {
                // keep at least one digit after the dot
                if (lastNonZero == dotPos) lastNonZero = dotPos + 1;
                if (lastNonZero + 1 < ePos) {
                    result.erase(lastNonZero + 1, ePos - lastNonZero - 1);
                    ePos = result.find('E');
                }
            }
        }

        // Clean up the exponent part
        if (ePos + 1 < result.size() && result[ePos + 1] == '+') {
            result.erase(ePos + 1, 1); // Remove '+' sign
            ePos = result.find('E'); // Recalculate ePos after modification
        }
        if (ePos + 1 < result.size() && result[ePos + 1] == '0' && ePos + 2 < result.size() && isdigit(result[ePos + 2])) {
            result.erase(ePos + 1, 1); // Remove leading zero in exponent
        }
    }

    // Allocate memory for the C string
    char* cstr = (char*)malloc(result.length() + 1);
    if (!cstr) return NULL;
    std::strcpy(cstr, result.c_str());

    return cstr;
}

char* convertDateTime(const char* input) {
    if (!input) return NULL;

    // Convert input to a C++ string for easier manipulation
    std::string dateTime(input);

    // Check if the input format is as expected: "YYYY-MM-DD HH:MM:SS"
    size_t spacePos = dateTime.find(' ');
    if (spacePos == std::string::npos || spacePos != 10 || dateTime.length() != 19) {
        return NULL; // Invalid format
    }

    // Replace the space with 'T'
    dateTime[spacePos] = 'T';

    // Allocate memory for the output C string
    char* result = (char*)malloc(dateTime.length() + 1);
    if (!result) return NULL;

    // Copy the modified string into the allocated memory
    std::strcpy(result, dateTime.c_str());

    return result;
}

extern "C" char* convertBool(const char* input) {
    // Check for null input
    if (!input) return NULL;

    // Convert input to a C++ string for easier manipulation
    std::string strInput(input);

    // Check for "true" cases
    if (strInput == "t" || strInput == "true" || strInput == "TRUE" || strInput == "1") {
        const char* trueStr = "true";
        char* result = (char*)malloc(strlen(trueStr) + 1);
        if (!result) return NULL;
        std::strcpy(result, trueStr);
        return result;
    }

    // Default case: "false"
    const char* falseStr = "false";
    char* result = (char*)malloc(strlen(falseStr) + 1);
    if (!result) return NULL;
    std::strcpy(result, falseStr);
    return result;
}


char* extract_second_iri(const char* input) {
    if (!input) {
        return copy_to_cstr("");
    }

    std::string iri(input);

    // Find the second occurrence of "http" that is followed by "://"
    size_t first = iri.find("http");
    size_t second = std::string::npos;
    if (first != std::string::npos) {
        size_t pos = first + 1;
        while (pos < iri.size()) {
            size_t candidate = iri.find("http", pos);
            if (candidate == std::string::npos) break;
            // only accept if followed by "://"
            if (iri.substr(candidate, 7) == "http://" || iri.substr(candidate, 8) == "https://") {
                second = candidate;
                break;
            }
            pos = candidate + 1;
        }
    }

    std::string result;

    if (second != std::string::npos) {
        result = "<"+iri.substr(second);
    } else {
        result = iri; // Return as-is if no second IRI
    }

    return copy_to_cstr(result);
}

char* clean_value(const char* input) {
    if (!input || std::strlen(input) == 0)
        return nullptr;

    std::string val(input);

    // Check for exactly ""
    if (val == "\"\"")
        return nullptr;

    // Lowercase copy to check for "null"
    std::string lower;
    lower.reserve(val.size());
    for (char c : val)
        lower += std::tolower(static_cast<unsigned char>(c));

    if (lower.find("null") != std::string::npos)
        return nullptr;

    // Allocate a copy to return
    char* result = new char[val.size() + 1];
    std::strcpy(result, val.c_str());
    return result;
}

// Trims leading and trailing whitespace from a string (handles MySQL CHAR padding)
char* trimString(const char* input) {
    if (!input) return NULL;
    std::string s(input);
    size_t start = s.find_first_not_of(" \t\r\n");
    if (start == std::string::npos) {
        char* result = (char*)malloc(1);
        if (!result) return NULL;
        result[0] = '\0';
        return result;
    }
    size_t end = s.find_last_not_of(" \t\r\n");
    std::string trimmed = s.substr(start, end - start + 1);
    char* result = (char*)malloc(trimmed.length() + 1);
    if (!result) return NULL;
    std::strcpy(result, trimmed.c_str());
    return result;
}

// Returns integer string as-is (values from DB are already integers)
char* toIntLiteral(const char* input) {
    if (!input) return NULL;
    char* result = (char*)malloc(strlen(input) + 1);
    if (!result) return NULL;
    std::strcpy(result, input);
    return result;
}

// Converts "YYYY-MM-DD" date string as-is (already in XSD date format)
char* convertDate(const char* input) {
    if (!input) return NULL;
    char* result = (char*)malloc(strlen(input) + 1);
    if (!result) return NULL;
    std::strcpy(result, input);
    return result;
}

char* startsWith(const char* input, const char* prefix) {
    return copy_to_cstr(starts_with_impl(to_string_safe(input), to_string_safe(prefix)) ? "1" : "0");
}

char* endsWith(const char* input, const char* suffix) {
    return copy_to_cstr(ends_with_impl(to_string_safe(input), to_string_safe(suffix)) ? "1" : "0");
}

char* containsStr(const char* input, const char* needle) {
    const std::string s = to_string_safe(input);
    const std::string n = to_string_safe(needle);
    return copy_to_cstr(s.find(n) != std::string::npos ? "1" : "0");
}

char* isAngleBracketed(const char* input) {
    const std::string s = to_string_safe(input);
    return copy_to_cstr((s.size() >= 2 && s.front() == '<' && s.back() == '>') ? "1" : "0");
}

char* isQuotedLiteral(const char* input) {
    const std::string s = to_string_safe(input);
    return copy_to_cstr((s.size() >= 2 && s.front() == '"' && s.back() == '"') ? "1" : "0");
}

char* isTypedLiteral(const char* input, const char* datatype) {
    const std::string s = to_string_safe(input);
    const std::string dt = to_string_safe(datatype);
    const std::string suffix = "^^<" + dt + ">";
    if (s.size() < suffix.size() + 2) return copy_to_cstr("0");
    if (!ends_with_impl(s, suffix)) return copy_to_cstr("0");
    std::string lit = s.substr(0, s.size() - suffix.size());
    return copy_to_cstr((lit.size() >= 2 && lit.front() == '"' && lit.back() == '"') ? "1" : "0");
}

char* isLanguageLiteral(const char* input) {
    const std::string s = to_string_safe(input);
    auto at = s.rfind('@');
    if (at == std::string::npos || at == 0 || at + 1 >= s.size()) return copy_to_cstr("0");
    std::string lit = s.substr(0, at);
    if (!(lit.size() >= 2 && lit.front() == '"' && lit.back() == '"')) return copy_to_cstr("0");
    return copy_to_cstr("1");
}

char* decodeIRI(const char* input) {
    const std::string s = to_string_safe(input);
    std::ostringstream out;
    for (size_t i = 0; i < s.size(); ++i) {
        if (s[i] == '%' && i + 2 < s.size() && is_hex_digit(s[i + 1]) && is_hex_digit(s[i + 2])) {
            std::string hex = s.substr(i + 1, 2);
            char decoded = static_cast<char>(std::strtol(hex.c_str(), nullptr, 16));
            out << decoded;
            i += 2;
        } else {
            out << s[i];
        }
    }
    return copy_to_cstr(out.str());
}

char* stripAngleBrackets(const char* input) {
    const std::string s = to_string_safe(input);
    if (s.size() >= 2 && s.front() == '<' && s.back() == '>')
        return copy_to_cstr(s.substr(1, s.size() - 2));
    return copy_to_cstr("");
}

char* addAngleBrackets(const char* input) {
    return copy_to_cstr("<" + to_string_safe(input) + ">");
}

char* removePrefix(const char* input, const char* prefix) {
    const std::string s = to_string_safe(input);
    const std::string p = to_string_safe(prefix);
    if (starts_with_impl(s, p)) return copy_to_cstr(s.substr(p.size()));
    return copy_to_cstr("");
}

char* removeSuffix(const char* input, const char* suffix) {
    const std::string s = to_string_safe(input);
    const std::string suf = to_string_safe(suffix);
    if (ends_with_impl(s, suf)) return copy_to_cstr(s.substr(0, s.size() - suf.size()));
    return copy_to_cstr("");
}

char* beforeFirst(const char* input, const char* delim) {
    const std::string s = to_string_safe(input);
    const std::string d = to_string_safe(delim);
    if (d.empty()) return copy_to_cstr("");
    size_t pos = s.find(d);
    if (pos == std::string::npos) return copy_to_cstr("");
    return copy_to_cstr(s.substr(0, pos));
}

char* afterFirst(const char* input, const char* delim) {
    const std::string s = to_string_safe(input);
    const std::string d = to_string_safe(delim);
    if (d.empty()) return copy_to_cstr("");
    size_t pos = s.find(d);
    if (pos == std::string::npos) return copy_to_cstr("");
    return copy_to_cstr(s.substr(pos + d.size()));
}

char* beforeLast(const char* input, const char* delim) {
    const std::string s = to_string_safe(input);
    const std::string d = to_string_safe(delim);
    if (d.empty()) return copy_to_cstr("");
    size_t pos = s.rfind(d);
    if (pos == std::string::npos) return copy_to_cstr("");
    return copy_to_cstr(s.substr(0, pos));
}

char* afterLast(const char* input, const char* delim) {
    const std::string s = to_string_safe(input);
    const std::string d = to_string_safe(delim);
    if (d.empty()) return copy_to_cstr("");
    size_t pos = s.rfind(d);
    if (pos == std::string::npos) return copy_to_cstr("");
    return copy_to_cstr(s.substr(pos + d.size()));
}

char* stripLiteralQuotes(const char* input) {
    const std::string s = to_string_safe(input);
    if (s.size() >= 2 && s.front() == '"' && s.back() == '"')
        return copy_to_cstr(s.substr(1, s.size() - 2));
    return copy_to_cstr("");
}

char* addLiteralQuotes(const char* input) {
    return copy_to_cstr("\"" + to_string_safe(input) + "\"");
}

char* stripTypedLiteral(const char* input, const char* datatype) {
    const std::string s = to_string_safe(input);
    const std::string dt = to_string_safe(datatype);
    const std::string suffix = "^^<" + dt + ">";
    if (!ends_with_impl(s, suffix)) return copy_to_cstr("");
    std::string lit = s.substr(0, s.size() - suffix.size());
    if (lit.size() >= 2 && lit.front() == '"' && lit.back() == '"')
        return copy_to_cstr(lit.substr(1, lit.size() - 2));
    return copy_to_cstr("");
}

char* makeTypedLiteral(const char* lexical, const char* datatype) {
    return copy_to_cstr("\"" + to_string_safe(lexical) + "\"^^<" + to_string_safe(datatype) + ">");
}

char* stripLanguageLiteral(const char* input) {
    const std::string s = to_string_safe(input);
    auto at = s.rfind('@');
    if (at == std::string::npos || at == 0 || at + 1 >= s.size()) return copy_to_cstr("");
    std::string lit = s.substr(0, at);
    if (lit.size() >= 2 && lit.front() == '"' && lit.back() == '"')
        return copy_to_cstr(lit.substr(1, lit.size() - 2));
    return copy_to_cstr("");
}

char* languageTag(const char* input) {
    const std::string s = to_string_safe(input);
    auto at = s.rfind('@');
    if (at == std::string::npos || at + 1 >= s.size()) return copy_to_cstr("");
    std::string lit = s.substr(0, at);
    if (lit.size() >= 2 && lit.front() == '"' && lit.back() == '"')
        return copy_to_cstr(s.substr(at + 1));
    return copy_to_cstr("");
}

char* concat2(const char* a, const char* b) {
    return copy_to_cstr(to_string_safe(a) + to_string_safe(b));
}

char* concat3(const char* a, const char* b, const char* c) {
    return copy_to_cstr(to_string_safe(a) + to_string_safe(b) + to_string_safe(c));
}

char* ifElse(const char* cond, const char* whenTrue, const char* whenFalse) {
    const std::string cv = to_string_safe(cond);
    return copy_to_cstr(cv == "1" ? to_string_safe(whenTrue) : to_string_safe(whenFalse));
}

} // end of extern "C"
