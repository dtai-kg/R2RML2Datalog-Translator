// SPDX-FileCopyrightText: 2026 Arcangelo Massari <arcangelo.massari@unibo.it>
//
// SPDX-License-Identifier: MIT

#include "../functors.cpp"
#include <cassert>
#include <iostream>
#include <souffle/datastructure/SymbolTableImpl.h>

int main() {
    souffle::SymbolTableImpl symbols;
    const auto checkUnary = [&symbols](auto functor, const std::string& input, const std::string& expected) {
        assert(symbols.decode(functor(&symbols, nullptr, symbols.encode(input))) == expected);
    };
    const auto checkBinary = [&symbols](auto functor, const std::string& input, const std::string& argument,
                                        const std::string& expected) {
        assert(symbols.decode(functor(&symbols, nullptr, symbols.encode(input), symbols.encode(argument))) == expected);
    };
    checkBinary(removePrefix, "a::b::c", "a::", "b::c");
    checkBinary(removePrefix, "abc", "x", "");
    checkBinary(removePrefix, "abc", "", "abc");
    checkBinary(removePrefix, "", "x", "");
    checkBinary(removePrefix, "", "", "");
    checkBinary(removePrefix, "abc", "abc", "");
    checkBinary(removeSuffix, "a::b::c", "::c", "a::b");
    checkBinary(removeSuffix, "abc", "x", "");
    checkBinary(removeSuffix, "abc", "", "abc");
    checkBinary(removeSuffix, "", "x", "");
    checkBinary(removeSuffix, "", "", "");
    checkBinary(removeSuffix, "abc", "abc", "");
    checkBinary(beforeFirst, "a::b::c", "::", "a");
    checkBinary(beforeFirst, "abc", "x", "");
    checkBinary(beforeFirst, "abc", "", "");
    checkBinary(beforeFirst, "", "x", "");
    checkBinary(beforeFirst, "", "", "");
    checkBinary(beforeFirst, "abc", "abc", "");
    checkBinary(afterFirst, "a::b::c", "::", "b::c");
    checkBinary(afterFirst, "abc", "x", "");
    checkBinary(afterFirst, "abc", "", "");
    checkBinary(afterFirst, "", "x", "");
    checkBinary(afterFirst, "", "", "");
    checkBinary(afterFirst, "abc", "abc", "");
    checkBinary(beforeLast, "a::b::c", "::", "a::b");
    checkBinary(beforeLast, "abc", "x", "");
    checkBinary(beforeLast, "abc", "", "");
    checkBinary(beforeLast, "", "x", "");
    checkBinary(beforeLast, "", "", "");
    checkBinary(beforeLast, "abc", "abc", "");
    checkUnary(decodeIRI, "", "");
    checkUnary(decodeIRI, "a%20b%2Fc", "a b/c");
    checkUnary(decodeIRI, "%C3%A8", "è");
    checkUnary(decodeIRI, "%zz%2", "%zz%2");
    checkUnary(decodeIRI, "a+b", "a+b");
    checkUnary(decodeIRI, "a%00b", "a");
    checkUnary(stripLiteralQuotes, "", "");
    checkUnary(stripLiteralQuotes, "\"\"", "");
    checkUnary(stripLiteralQuotes, "\"hello\"", "hello");
    checkUnary(stripLiteralQuotes, "hello", "");
    checkUnary(stripLiteralQuotes, "\"hello\"@en", "");
    checkUnary(stripLiteralQuotes, "\"a\\\"b\"", "a\\\"b");
    checkBinary(stripTypedLiteral, "\"42\"^^<urn:int>", "urn:int", "42");
    checkBinary(stripTypedLiteral, "\"\"^^<urn:int>", "urn:int", "");
    checkBinary(stripTypedLiteral, "\"42\"^^<urn:int>", "urn:other", "");
    checkBinary(stripTypedLiteral, "42^^<urn:int>", "urn:int", "");
    checkBinary(stripTypedLiteral, "", "", "");
    checkBinary(stripTypedLiteral, "\"x\"^^<>", "", "x");
    const std::string longValue(8192, 'x');
    const auto value = symbols.encode(longValue);
    const auto empty = symbols.encode("");
    const auto decorated = symbols.encode("pre" + longValue + "end");
    const auto prefix = symbols.encode("pre");
    const auto suffix = symbols.encode("end");
    const auto quoted = symbols.encode("\"" + longValue + "\"");
    const auto typed = symbols.encode("\"" + longValue + "\"^^<urn:type>");
    const auto datatype = symbols.encode("urn:type");
    symbols.encode("pre" + longValue);
    symbols.encode(longValue + "end");
    const auto countSymbols = [&symbols]() {
        std::size_t count = 0;
        for (auto iterator = symbols.begin(); iterator != symbols.end(); ++iterator)
            ++count;
        return count;
    };
    const auto symbolCount = countSymbols();
    for (int iteration = 0; iteration < 10000; ++iteration) {
        assert(removePrefix(&symbols, nullptr, value, empty) == value);
        assert(removeSuffix(&symbols, nullptr, value, empty) == value);
        assert(symbols.decode(beforeFirst(&symbols, nullptr, decorated, suffix)) == "pre" + longValue);
        assert(symbols.decode(afterFirst(&symbols, nullptr, decorated, prefix)) == longValue + "end");
        assert(symbols.decode(beforeLast(&symbols, nullptr, decorated, suffix)) == "pre" + longValue);
        assert(decodeIRI(&symbols, nullptr, value) == value);
        assert(stripLiteralQuotes(&symbols, nullptr, quoted) == value);
        assert(stripTypedLiteral(&symbols, nullptr, typed, datatype) == value);
    }
    assert(countSymbols() == symbolCount);
    std::cout << "48 exact cases and 80000 long-string calls passed\n";
}
