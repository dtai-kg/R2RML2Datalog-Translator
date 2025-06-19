#include <iostream>
#include <string>
#include <sstream>
#include <iomanip>
#include <cstring>
#include <limits>
#include <regex>

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
            if (lastNonZero != std::string::npos && lastNonZero > dotPos) {
                result.erase(lastNonZero + 1, ePos - lastNonZero - 1); // Remove trailing zeros
                ePos = result.find('E'); // Recalculate ePos after modification
            }
            if (dotPos < result.size() && result[dotPos + 1] == 'E') {
                result.erase(dotPos, 1); // Remove the dot if no decimals remain
                ePos = result.find('E'); // Recalculate ePos after modification
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
    std::string iri(input);

    // Find the second occurrence of "http"
    size_t first = iri.find("http");
    size_t second = iri.find("http", first + 1);

    std::string result;

    if (second != std::string::npos) {
        result = "<"+iri.substr(second);
    } else {
        result = iri; // Return as-is if no second IRI
    }

    // Allocate and copy result
    char* output = (char*) std::malloc(result.size() + 1);
    std::strcpy(output, result.c_str());
    return output;
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





} // end of extern "C"
