# About
A small parser using JAVA SAX library to parse and filter Wikipedia modifications dumps.

# Requirements

Wikipedia modifications dump:
https://wicopaco.limsi.fr/corpus/wrhc-fr_070101-sch1-v2.xml.gz

XML Schema for modifications dump:
https://wicopaco.limsi.fr/corpus/wrhc-sch1.xsd

Spelling errors annotations file for revisions:
https://wicopaco.limsi.fr/corpus/spelling_error-v3.xml

# Usage

```
usage: MyParser [options] [parameters] <file-path> <error-file-path>
parameters:
    --error-type: string, type of errors: non_word_error, real_word_error, any (default: any)
    --min-user-count: number, minimal contributions for users (default: -1)
    --questions-count: number, how many question to extract from file (default: -1)
options:
    --no-anon: no anonymous users contributions
```