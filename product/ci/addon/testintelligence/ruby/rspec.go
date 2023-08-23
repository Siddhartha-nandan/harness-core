// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

/*
Package python
Any Python application that can run through the rspec CLI
should be able to use this to perform test intelligence.

Test filtering:
rspec test
*/
package ruby

import (
	"context"
	"errors"

	"github.com/harness/harness-core/commons/go/lib/exec"
	"github.com/harness/harness-core/commons/go/lib/filesystem"
	"github.com/harness/harness-core/commons/go/lib/utils"
	"github.com/harness/ti-client/types"
	"go.uber.org/zap"
)

type rspecRunner struct {
	fs                filesystem.FileSystem
	log               *zap.SugaredLogger
	cmdContextFactory exec.CmdContextFactory
	agentPath         string
}

func NewRspecRunner(log *zap.SugaredLogger, fs filesystem.FileSystem, factory exec.CmdContextFactory, agentPath string) *rspecRunner {
	return &rspecRunner{
		fs:                fs,
		log:               log,
		cmdContextFactory: factory,
		agentPath:         agentPath,
	}
}

func (b *rspecRunner) AutoDetectPackages() ([]string, error) {
	return []string{}, errors.New("not implemented")
}

func (b *rspecRunner) AutoDetectTests(ctx context.Context, testGlobs []string) ([]types.RunnableTest, error) {
	if len(testGlobs) == 0 {
		testGlobs = utils.RUBY_TEST_PATTERN
	}
	return utils.GetTestsFromLocal(testGlobs, "rb", utils.LangType_RUBY)
}

func (b *rspecRunner) ReadPackages(files []types.File) []types.File {
	return files
}

func (b *rspecRunner) GetCmd(ctx context.Context, tests []types.RunnableTest, userArgs, agentConfigPath string, ignoreInstr, runAll bool) (string, error) {
	// Placeholder
	testCmd := ""
	return testCmd, nil
}
