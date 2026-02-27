var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":20,"id":1244,"methods":[{"el":13,"sc":2,"sl":10},{"el":19,"sc":2,"sl":15}],"name":"NumberSource","sl":3}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_151":{"methods":[{"sl":15}],"name":"conflictNumberOfRows","pass":true,"statements":[{"sl":18}]},"test_199":{"methods":[{"sl":15}],"name":"conflictNumberOfRows","pass":true,"statements":[{"sl":18}]},"test_206":{"methods":[{"sl":15}],"name":"conflictNumberOfRows","pass":true,"statements":[{"sl":18}]},"test_34":{"methods":[{"sl":15}],"name":"conflictNumberOfRows","pass":true,"statements":[{"sl":18}]},"test_56":{"methods":[{"sl":15}],"name":"conflictNumberOfRows","pass":true,"statements":[{"sl":18}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [199, 34, 56, 206, 151], [], [], [199, 34, 56, 206, 151], [], []]
