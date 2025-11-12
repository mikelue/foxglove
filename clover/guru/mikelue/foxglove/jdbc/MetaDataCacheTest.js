var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":76,"id":2617,"methods":[{"el":31,"sc":2,"sl":31},{"el":34,"sc":2,"sl":33},{"el":37,"sc":2,"sl":36},{"el":58,"sc":23,"sl":53},{"el":75,"sc":2,"sl":42}],"name":"MetaDataCacheTest","sl":23}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_195":{"methods":[{"sl":42},{"sl":53}],"name":"[1] col_existing, col_existing, true","pass":true,"statements":[{"sl":53},{"sl":54},{"sl":55},{"sl":60},{"sl":61},{"sl":65},{"sl":66},{"sl":67}]},"test_287":{"methods":[{"sl":42},{"sl":53}],"name":"[3] col_not_existing, col_existing, false","pass":true,"statements":[{"sl":53},{"sl":54},{"sl":55},{"sl":60},{"sl":61},{"sl":65},{"sl":69}]},"test_71":{"methods":[{"sl":42},{"sl":53}],"name":"[2] col_existing, COL_EXISTING, true","pass":true,"statements":[{"sl":53},{"sl":54},{"sl":55},{"sl":60},{"sl":61},{"sl":65},{"sl":66},{"sl":67}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [195, 71, 287], [], [], [], [], [], [], [], [], [], [], [195, 71, 287], [195, 71, 287], [195, 71, 287], [], [], [], [], [195, 71, 287], [195, 71, 287], [], [], [], [195, 71, 287], [195, 71], [195, 71], [], [287], [], [], [], [], [], [], []]
