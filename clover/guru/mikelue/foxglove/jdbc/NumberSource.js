var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":20,"id":1244,"methods":[{"el":13,"sc":2,"sl":10},{"el":19,"sc":2,"sl":15}],"name":"NumberSource","sl":3}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_211":{"methods":[{"sl":15}],"name":"conflictNumberOfRows","pass":true,"statements":[{"sl":18}]},"test_215":{"methods":[{"sl":15}],"name":"conflictNumberOfRows","pass":true,"statements":[{"sl":18}]},"test_25":{"methods":[{"sl":15}],"name":"conflictNumberOfRows","pass":true,"statements":[{"sl":18}]},"test_253":{"methods":[{"sl":10}],"name":"onTupleGenerated","pass":true,"statements":[{"sl":12}]},"test_44":{"methods":[{"sl":15}],"name":"conflictNumberOfRows","pass":true,"statements":[{"sl":18}]},"test_92":{"methods":[{"sl":15}],"name":"conflictNumberOfRows","pass":true,"statements":[{"sl":18}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [253], [], [253], [], [], [92, 44, 215, 25, 211], [], [], [92, 44, 215, 25, 211], [], []]
