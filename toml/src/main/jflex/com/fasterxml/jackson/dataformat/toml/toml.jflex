package com.fasterxml.jackson.dataformat.toml;

%%

%class Lexer
%type TomlToken
%unicode
%line
%column
%char
%buffer 4000

%ctorarg com.fasterxml.jackson.core.io.IOContext ioContext
%ctorarg TomlStreamReadException.ErrorContext errorContext

%init{
this.ioContext = ioContext;
this.streamReadConstraints = ioContext.streamReadConstraints();
this.errorContext = errorContext;
yybegin(EXPECT_EXPRESSION);
this.zzBuffer = ioContext.allocTokenBuffer();
this.textBuffer = ioContext.constructReadConstrainedTextBuffer();
%init}

%{
  private final com.fasterxml.jackson.core.io.IOContext ioContext;
  private final TomlStreamReadException.ErrorContext errorContext;

  boolean prohibitInternalBufferAllocate = false;
  private boolean releaseTokenBuffer = true;

  private boolean trimmedNewline;
  final com.fasterxml.jackson.core.util.TextBuffer textBuffer;
  private final com.fasterxml.jackson.core.StreamReadConstraints streamReadConstraints;
  private int nestingDepth;

  private void requestLargerBuffer() throws TomlStreamReadException {
      if (prohibitInternalBufferAllocate) {
          throw errorContext.atPosition(this).generic("Token too long, but buffer resizing prohibited");
      }

      // todo: use recycler
      char[] newBuffer = new char[zzBuffer.length * 2];
      System.arraycopy(zzBuffer, 0, newBuffer, 0, zzBuffer.length);
      if (releaseTokenBuffer) {
          ioContext.releaseTokenBuffer(zzBuffer);
          releaseTokenBuffer = false;
      }
      zzBuffer = newBuffer;
  }

  public void releaseBuffers() {
      if (releaseTokenBuffer) {
          ioContext.releaseTokenBuffer(zzBuffer);
          zzBuffer = null;
      }
      textBuffer.releaseBuffers();
  }

  public int getNestingDepth() {
      return nestingDepth;
  }

  private void startString() {
      // resetWithEmpty does not set _currentSegment, so we need this variant to be able to append further data
      textBuffer.emptyAndGetCurrentSegment();
      trimmedNewline = false;
  }

  private void appendNormalTextToken() throws java.io.IOException {
      // equivalent to append(yytext()), without the roundtrip through the String constructor
      textBuffer.append(zzBuffer, zzStartRead, zzMarkedPos-zzStartRead);
  }

  private void appendNewlineWithPossibleTrim() throws java.io.IOException {
      if (!trimmedNewline && textBuffer.size() == 0) {
          trimmedNewline = true;
      } else {
          // \n or \r\n todo: "TOML parsers should feel free to normalize newline to whatever makes sense for their platform."
          appendNormalTextToken();
      }
  }

  private void appendUnicodeEscapeShort() throws java.io.IOException {
      int value = (Character.digit(yycharat(2), 16) << 12) |
                   (Character.digit(yycharat(3), 16) << 8) |
                   (Character.digit(yycharat(4), 16) << 4) |
                   Character.digit(yycharat(5), 16);
      textBuffer.append((char) value);
  }

  private void appendUnicodeEscapeLong() throws java.io.IOException {
     int value = (Character.digit(yycharat(2), 16) << 28) |
                 (Character.digit(yycharat(3), 16) << 24) |
                 (Character.digit(yycharat(4), 16) << 20) |
                 (Character.digit(yycharat(5), 16) << 16) |
                 (Character.digit(yycharat(6), 16) << 12) |
                 (Character.digit(yycharat(7), 16) << 8) |
                 (Character.digit(yycharat(8), 16) << 4) |
                 Character.digit(yycharat(9), 16);
     if (Character.isValidCodePoint(value)) {
         // "Such code points can be represented using a single char."
         if (value <= Character.MAX_VALUE) {
             textBuffer.append((char) value);
         } else {
             textBuffer.append(Character.highSurrogate(value));
             textBuffer.append(Character.lowSurrogate(value));
         }
     } else {
         throw errorContext.atPosition(this).generic("Invalid code point " + Integer.toHexString(value));
     }
  }

  int getLine() { return yyline; }
  int getColumn() { return yycolumn; }
  long getCharPos() { return yychar; }

  char[] getTextBuffer() { return zzBuffer; };
  int getTextBufferStart() { return zzStartRead; };
  int getTextBufferEnd() { return zzMarkedPos; };
%}

Ws = [ \t]*
WsNonEmpty = [ \t]+
NewLine = \n|\r\n
CommentStartSymbol = "#"
NonEol = [^\u0000-\u0008\u000a-\u001f\u007f]

//Expression = {Ws} (({KeyVal}|{Table}) {Ws}) {Comment}?
Comment = {CommentStartSymbol} {NonEol}*

//KeyVal = {Key} {KeyValSep} {Val}
//Key = {SimpleKey} | {DottedKey}
KeyValSep = {Ws} "=" {Ws}
//SimpleKey = {QuotedKey} | {UnquotedKey}

UnquotedKey = [A-Za-z0-9\-_]+
//QuotedKey = {BasicString} | {LiteralString}
//DottedKey = {SimpleKey} ({Ws} "." {Ws} {SimpleKey})+
DotSep = {Ws} "." {Ws}

// grammar rule
// Val = {String} | {Boolean} | {Array} | {InlineTable} | {DateTime} | {Float} | {Integer}

//String = {MlBasicString} | {BasicString} | {MlLiteralString} | {LiteralString}

//BasicString = {QuotationMark} {BasicChar}* {QuotationMark}
QuotationMark = "\""
//BasicChar = {BasicUnescaped} | {Escaped}
// exclude control chars (tab is allowed, " and \)
//BasicUnescaped = [^\u0000-\u0008\u0009-\u001f\u007f\\\"]
//Escaped = "\\" ([\"\\bfnrt]|"u" {HexDig} {HexDig} {HexDig} {HexDig} ({HexDig} {HexDig} {HexDig} {HexDig})?)
UnicodeEscapeShort = "\\u" {HexDig} {HexDig} {HexDig} {HexDig}
UnicodeEscapeLong = "\\U" {HexDig} {HexDig} {HexDig} {HexDig} {HexDig} {HexDig} {HexDig} {HexDig}

//MlBasicString = {MlBasicStringDelim} {NewLine}? {MlBasicBody} {MlBasicStringDelim}
MlBasicStringDelim = "\"\"\""
//MlBasicBody = {MlbContent}* ({MlbQuotes} {MlbContent}+)* {MlbQuotes}?
//MlbContent = {MlbChar} | {NewLine} | {MlbEscapedNl}
//MlbChar = {MlbUnescaped} | {Escaped}
//MlbUnescaped = {BasicUnescaped}
//MlbEscapedNl = {Escaped} {Ws} {NewLine} ([ \t] | {NewLine})*

Apostrophe = "'"

MlLiteralStringDelim = "'''"

Integer = {DecInt} | {HexInt} | {OctInt} | {BinInt}
DecInt = [+-]? {UnsignedDecInt}
UnsignedDecInt = [0-9] | ([1-9] (_? [0-9])+)
HexInt = 0x {HexDig} (_? {HexDig})*
OctInt = 0o [0-7] (_? [0-7])*
BinInt = 0b [01] (_? [01])*

Float = {DecInt} ({Exp} | {Frac} {Exp}?) | {SpecialFloat}
Frac = "." {ZeroPrefixableInt}
ZeroPrefixableInt = [0-9] (_? [0-9])*
Exp = [eE] [+-]? {ZeroPrefixableInt}
SpecialFloat = [+-]? (inf | nan)

// Boolean = "true" | "false"

//DateTime = {OffsetDateTime} | {LocalDateTime} | {LocalDate} | {LocalTime}
DateFullyear = [0-9][0-9][0-9][0-9]
DateMonth = [0-9][0-9]
DateMday = [0-9][0-9]
TimeDelim = [Tt ]
TimeHour = [0-9][0-9]
TimeMinute = [0-9][0-9]
TimeSecond = [0-9][0-9]
TimeSecfrac = "." [0-9]+
TimeNumoffset = [+-] {TimeHour} ":" {TimeMinute}
TimeOffset = "Z" | {TimeNumoffset}
PartialTime = {TimeHour} ":" {TimeMinute} ":" {TimeSecond} {TimeSecfrac}?
FullDate = {DateFullyear} "-" {DateMonth} "-" {DateMday}
FullTime = {PartialTime} {TimeOffset}

OffsetDateTime = {FullDate} {TimeDelim} {FullTime}
LocalDateTime = {FullDate} {TimeDelim} {PartialTime}
LocalDate = {FullDate}
LocalTime = {PartialTime}

//Array = {ArrayOpen} {ArrayValues}? {WsCommentNewline} {ArrayClose}
ArrayOpen = "["
ArrayClose = "]"
//ArrayValues = {WsCommentNewline} {Val} {WsCommentNewline} ("," {ArrayValues} | ","?)
Comma = ","
WsCommentNewlineNonEmpty = ([\t ] | {Comment}? {NewLine})+

//Table = {StdTable} | {ArrayTable}

//StdTable = {StdTableClose} {Key} {StdTableClose}
StdTableOpen = "[" {Ws}
StdTableClose = {Ws} "]"

//InlineTable = {InlineTableOpen} {InlineTableKeyvals}? {InlineTableClose}
InlineTableOpen = "{" {Ws}
InlineTableClose = {Ws} "}"
//InlineTableKeyvals = {KeyVal} ("," {InlineTableKeyvals})?

//ArrayTable = {ArrayTableOpen} {Key} {ArrayTableClose}
ArrayTableOpen = "[[" {Ws}
ArrayTableClose = {Ws} "]]"

HexDig = [0-9A-Fa-f]

%state EXPECT_EXPRESSION
%state EXPECT_INLINE_KEY
%state EXPECT_VALUE
%state EXPECT_EOL
%state EXPECT_ARRAY_SEP
%state EXPECT_TABLE_SEP

%state ML_BASIC_STRING
%state BASIC_STRING
%state ML_LITERAL_STRING
%state LITERAL_STRING

%%

<EXPECT_EXPRESSION> {
    // this state matches until the *first* simple-key of a key, or until the -open token of a table.

    // toml = expression *( newline expression )
    // expression =  ws [ comment ]

    // expression =/ ws keyval ws [ comment ]
    // keyval = key keyval-sep val
    // key = simple-key / dotted-key
    // simple-key = quoted-key / unquoted-key

    // expression =/ ws table ws [ comment ]
    // table = std-table / array-table
    // std-table = std-table-open key std-table-close
    // array-table = array-table-open key array-table-close

    {UnquotedKey} {return TomlToken.UNQUOTED_KEY;}
    // quoted-key = basic-string / literal-string
    {QuotationMark} {
          yybegin(BASIC_STRING);
          startString();
      }
    {Apostrophe} {
          yybegin(LITERAL_STRING);
          startString();
      }
    {StdTableOpen} {
          streamReadConstraints.validateNestingDepth(++nestingDepth);
          return TomlToken.STD_TABLE_OPEN;
      }
    {ArrayTableOpen} {
          streamReadConstraints.validateNestingDepth(++nestingDepth);
          return TomlToken.ARRAY_TABLE_OPEN;
      }
    {KeyValSep} {return TomlToken.KEY_VAL_SEP;}
    {NewLine} {}
    {Comment} {}
    {WsNonEmpty} {}
}

<EXPECT_INLINE_KEY> {
    // this state matches a possibly dotted key, until a following token (keyval-sep, std-table-close, array-table-close)

    // key = simple-key / dotted-key
    // dotted-key = simple-key 1*( dot-sep simple-key )
    // simple-key = quoted-key / unquoted-key

    {UnquotedKey} {return TomlToken.UNQUOTED_KEY;}
    {DotSep} {return TomlToken.DOT_SEP;}
    // quoted-key = basic-string / literal-string
    {QuotationMark} {
          yybegin(BASIC_STRING);
          startString();
      }
    {Apostrophe} {
          yybegin(LITERAL_STRING);
          startString();
      }
    {KeyValSep} {return TomlToken.KEY_VAL_SEP;}
    {InlineTableClose} {
          nestingDepth--;
          return TomlToken.INLINE_TABLE_CLOSE;
      }
    {StdTableClose} {
          nestingDepth--;
          return TomlToken.STD_TABLE_CLOSE;
      }
    {ArrayTableClose} {
          nestingDepth--;
          return TomlToken.ARRAY_TABLE_CLOSE;
      }
}

<EXPECT_EOL> {
    // this matches the remainder after a keyval or table in an expression.

    // expression =  ws [ comment ]
    // expression =/ ws keyval ws [ comment ]
    // expression =/ ws table ws [ comment ]

    {NewLine} {yybegin(EXPECT_EXPRESSION);}
    {Comment} {}
    {WsNonEmpty} {}
}

<EXPECT_VALUE> {
    // val = string / boolean / array / inline-table / date-time / float / integer
    // used by:
    // keyval = key keyval-sep val
    // array-values =  ws-comment-newline val ws-comment-newline array-sep array-values
    // array-values =/ ws-comment-newline val ws-comment-newline [ array-sep ]

    // strings
    {QuotationMark} {
          yybegin(BASIC_STRING);
          startString();
      }
    {MlBasicStringDelim} {
          yybegin(ML_BASIC_STRING);
          startString();
      }
    {Apostrophe} {
          yybegin(LITERAL_STRING);
          startString();
      }
    {MlLiteralStringDelim} {
          yybegin(ML_LITERAL_STRING);
          startString();
      }

    // scalar values
    true {return TomlToken.TRUE;}
    false {return TomlToken.FALSE;}
    {OffsetDateTime} {return TomlToken.OFFSET_DATE_TIME;}
    {LocalDateTime} {return TomlToken.LOCAL_DATE_TIME;}
    {LocalDate} {return TomlToken.LOCAL_DATE;}
    {LocalTime} {return TomlToken.LOCAL_TIME;}
    {Float} {return TomlToken.FLOAT;}
    {Integer} {return TomlToken.INTEGER;}
    [+-]? [0-9]+ {
        throw errorContext.atPosition(this).generic("Zero-prefixed ints are not valid. If you want an octal literal, use the prefix '0o'");
      }

    // inline array / table
    {ArrayOpen} {WsCommentNewlineNonEmpty}* {
          streamReadConstraints.validateNestingDepth(++nestingDepth);
          return TomlToken.ARRAY_OPEN;
      }
    {InlineTableOpen} {
          streamReadConstraints.validateNestingDepth(++nestingDepth);
          return TomlToken.INLINE_TABLE_OPEN;
      }

    // array end just after comma
    {WsCommentNewlineNonEmpty}* {ArrayClose} {
          nestingDepth--;
          return TomlToken.ARRAY_CLOSE;
      }
}

<EXPECT_ARRAY_SEP> {
    // array-values =  ws-comment-newline val ws-comment-newline array-sep array-values
    // array-values =/ ws-comment-newline val ws-comment-newline [ array-sep ]
    {Comma} {WsCommentNewlineNonEmpty}* {return TomlToken.COMMA;}
    {ArrayClose} {
          nestingDepth--;
          return TomlToken.ARRAY_CLOSE;
      }
    {WsCommentNewlineNonEmpty} {} // always allowed here
}

<EXPECT_TABLE_SEP> {
    // inline-table = inline-table-open [ inline-table-keyvals ] inline-table-close
    // inline-table-keyvals = keyval [ inline-table-sep inline-table-keyvals ]

    {Ws} {Comma} {Ws} {return TomlToken.COMMA;}
    {InlineTableClose} {
          nestingDepth--;
          return TomlToken.INLINE_TABLE_CLOSE;
      }
}

<BASIC_STRING> {
    // basic-string = quotation-mark *basic-char quotation-mark
    // basic-char = basic-unescaped / escaped
    // basic-unescaped = wschar / %x21 / %x23-5B / %x5D-7E / non-ascii
    {QuotationMark} {return TomlToken.STRING;}
}

<ML_BASIC_STRING> {
    // ml-basic-string = ml-basic-string-delim [ newline ] ml-basic-body ml-basic-string-delim
    // ml-basic-body = *mlb-content *( mlb-quotes 1*mlb-content ) [ mlb-quotes ]
    // mlb-content = mlb-char / newline / mlb-escaped-nl
    // mlb-char = mlb-unescaped / escaped
    // mlb-quotes = 1*2quotation-mark
    {MlBasicStringDelim} {return TomlToken.STRING;}
    {NewLine} { appendNewlineWithPossibleTrim(); }
    // mlb-quotes: inline
    \" { textBuffer.append('"'); }
    // mlb-quotes: at the end
    \" {MlBasicStringDelim} {
          textBuffer.append('"');
          return TomlToken.STRING;
      }
    \"\" {MlBasicStringDelim} {
          textBuffer.append('"');
          textBuffer.append('"');
          return TomlToken.STRING;
      }
    // mlb-escaped-nl
    // ignore, but disable newline trimming after it
    \\ {Ws} {NewLine} ([ \t] | {NewLine})* { trimmedNewline = true; }
}

<BASIC_STRING, ML_BASIC_STRING> {
    [^\u0000-\u0008\u000a-\u001f\u007f\\\"]+ { appendNormalTextToken(); }
    \\\" { textBuffer.append('"'); }
    \\\\ { textBuffer.append('\\'); }
    \\b { textBuffer.append('\b'); }
    \\f { textBuffer.append('\f'); }
    \\n { textBuffer.append('\n'); }
    \\r { textBuffer.append('\r'); }
    \\t { textBuffer.append('\t'); }
    {UnicodeEscapeShort} { appendUnicodeEscapeShort(); }
    {UnicodeEscapeLong} { appendUnicodeEscapeLong(); }
    \\ { throw errorContext.atPosition(this).generic("Unknown escape sequence"); }
}

<LITERAL_STRING> {
    // literal-string = apostrophe *literal-char apostrophe
    {Apostrophe} {return TomlToken.STRING;}
    [^\u0000-\u0008\u000a-\u001f\u007f']+ { appendNormalTextToken(); }
}

<ML_LITERAL_STRING> {
    // ml-literal-string = ml-literal-string-delim [ newline ] ml-literal-body ml-literal-string-delim
    // ml-literal-body = *mll-content *( mll-quotes 1*mll-content ) [ mll-quotes ]
    // mll-quotes = 1*2apostrophe
    {MlLiteralStringDelim} {return TomlToken.STRING;}
    [^\u0000-\u0008\u000a-\u001f\u007f']+ { appendNormalTextToken(); }
    {NewLine} { appendNewlineWithPossibleTrim(); }
    // mll-quotes: inline
    {Apostrophe} { textBuffer.append('\''); }
    // mll-quotes: at the end
    {Apostrophe} {MlLiteralStringDelim} {
          textBuffer.append('\'');
          return TomlToken.STRING;
      }
    {Apostrophe}{Apostrophe} {MlLiteralStringDelim} {
          textBuffer.append('\'');
          textBuffer.append('\'');
          return TomlToken.STRING;
      }
}

// catchall error rules. Must never match more than one character, so that they cannot take precedent over other rules.
[\r\n] {
  throw errorContext.atPosition(this).generic("Newline not permitted here");
}
[\u0000-\u001f\u007f] {
  throw errorContext.atPosition(this).generic("Illegal control character");
}
\# {
  throw errorContext.atPosition(this).generic("Comment not permitted here");
}
<EXPECT_EOL, EXPECT_ARRAY_SEP, EXPECT_TABLE_SEP> [^] {
  throw errorContext.atPosition(this).generic("More data after value has already ended. Invalid value preceding this position?");
}
[^] {
  throw errorContext.atPosition(this).generic("Unknown token");
}
