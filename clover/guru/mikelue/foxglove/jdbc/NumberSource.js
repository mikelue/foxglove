var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":20,"id":1252,"methods":[{"el":13,"sc":2,"sl":10},{"el":19,"sc":2,"sl":15}],"name":"NumberSource","sl":3}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_159":{"methods":[{"sl":15}],"name":"conflictNumberOfRows","pass":true,"statements":[{"sl":18}]},"test_174":{"methods":[{"sl":15}],"name":"conflictNumberOfRows","pass":true,"statements":[{"sl":18}]},"test_206":{"methods":[{"sl":15}],"name":"conflictNumberOfRows","pass":true,"statements":[{"sl":18}]},"test_207":{"methods":[{"sl":15}],"name":"conflictNumberOfRows","pass":true,"statements":[{"sl":18}]},"test_270":{"methods":[{"sl":15}],"name":"conflictNumberOfRows","pass":true,"statements":[{"sl":18}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [207, 270, 174, 159, 206], [], [], [207, 270, 174, 159, 206], [], []]
