// Copyright 2022 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package python

import (
	"fmt"

	"github.com/harness/harness-core/commons/go/lib/utils"
	"github.com/harness/harness-core/product/ci/common/external"
	"github.com/harness/harness-core/product/ci/ti-service/types"
)

var (
	getFiles     = utils.GetFiles
	getWorkspace = external.GetWrkspcPath
	defaultTestGlobs = []string{"test_*.py", "*_test.py"}
)

// GetPythonTests returns list of RunnableTests in the workspace with cs extension.
// In case of errors, return empty list
func GetPythonTests(testGlobs []string) ([]types.RunnableTest, error) {
	tests := make([]types.RunnableTest, 0)
	wp, err := getWorkspace()
	if err != nil {
		return tests, err
	}

	files, _ := getFiles(fmt.Sprintf("%s/**/*.py", wp))
	if len(testGlobs) == 0 {
		testGlobs = defaultTestGlobs
	}
	for _, path := range files {
		if path == "" {
			continue
		}
		f := types.File{Name: path}
		node, _ := utils.ParsePythonNode(f, testGlobs)
		if node.Type != utils.NodeType_TEST {
			continue
		}
		test := types.RunnableTest{
			Class: node.File,
		}
		tests = append(tests, test)
	}
	return tests, nil
}
