// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

// Code generated by MockGen. DO NOT EDIT.
// Source: plugin.go

// Package steps is a generated GoMock package.
package steps

import (
	context "context"
	gomock "github.com/golang/mock/gomock"
	"github.com/harness/harness-core/product/ci/engine/output"
	reflect "reflect"
)

// MockPluginStep is a mock of PluginStep interface.
type MockPluginStep struct {
	ctrl     *gomock.Controller
	recorder *MockPluginStepMockRecorder
}

// MockPluginStepMockRecorder is the mock recorder for MockPluginStep.
type MockPluginStepMockRecorder struct {
	mock *MockPluginStep
}

// NewMockPluginStep creates a new mock instance.
func NewMockPluginStep(ctrl *gomock.Controller) *MockPluginStep {
	mock := &MockPluginStep{ctrl: ctrl}
	mock.recorder = &MockPluginStepMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockPluginStep) EXPECT() *MockPluginStepMockRecorder {
	return m.recorder
}

// Run mocks base method.
func (m *MockPluginStep) Run(ctx context.Context) (*output.StepOutput, int32, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Run", ctx)
	ret0, _ := ret[0].(*output.StepOutput)
	ret1, _ := ret[1].(int32)
	ret2, _ := ret[2].(error)
	return ret0, ret1, ret2
}

// Run indicates an expected call of Run.
func (mr *MockPluginStepMockRecorder) Run(ctx interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Run", reflect.TypeOf((*MockPluginStep)(nil).Run), ctx)
}
