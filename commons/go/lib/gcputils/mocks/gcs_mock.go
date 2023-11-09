// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

// Code generated by MockGen. DO NOT EDIT.
// Source: gcs.go

// Package gcputils is a generated GoMock package.
package gcputils

import (
	context "context"
	reflect "reflect"

	gomock "github.com/golang/mock/gomock"
)

// MockGCS is a mock of GCS interface.
type MockGCS struct {
	ctrl     *gomock.Controller
	recorder *MockGCSMockRecorder
}

// MockGCSMockRecorder is the mock recorder for MockGCS.
type MockGCSMockRecorder struct {
	mock *MockGCS
}

// NewMockGCS creates a new mock instance.
func NewMockGCS(ctrl *gomock.Controller) *MockGCS {
	mock := &MockGCS{ctrl: ctrl}
	mock.recorder = &MockGCSMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockGCS) EXPECT() *MockGCSMockRecorder {
	return m.recorder
}

// Close mocks base method.
func (m *MockGCS) Close() error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Close")
	ret0, _ := ret[0].(error)
	return ret0
}

// Close indicates an expected call of Close.
func (mr *MockGCSMockRecorder) Close() *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Close", reflect.TypeOf((*MockGCS)(nil).Close))
}

// DeleteObject mocks base method.
func (m *MockGCS) DeleteObject(ctx context.Context, bucketName, objectName string) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "DeleteObject", ctx, bucketName, objectName)
	ret0, _ := ret[0].(error)
	return ret0
}

// DeleteObject indicates an expected call of DeleteObject.
func (mr *MockGCSMockRecorder) DeleteObject(ctx, bucketName, objectName interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "DeleteObject", reflect.TypeOf((*MockGCS)(nil).DeleteObject), ctx, bucketName, objectName)
}

// DownloadObject mocks base method.
func (m *MockGCS) DownloadObject(ctx context.Context, bucketName, objectName, dstFilePath string) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "DownloadObject", ctx, bucketName, objectName, dstFilePath)
	ret0, _ := ret[0].(error)
	return ret0
}

// DownloadObject indicates an expected call of DownloadObject.
func (mr *MockGCSMockRecorder) DownloadObject(ctx, bucketName, objectName, dstFilePath interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "DownloadObject", reflect.TypeOf((*MockGCS)(nil).DownloadObject), ctx, bucketName, objectName, dstFilePath)
}

// GetObjectMetadata mocks base method.
func (m *MockGCS) GetObjectMetadata(ctx context.Context, bucketName, objectName string) (map[string]string, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "GetObjectMetadata", ctx, bucketName, objectName)
	ret0, _ := ret[0].(map[string]string)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// GetObjectMetadata indicates an expected call of GetObjectMetadata.
func (mr *MockGCSMockRecorder) GetObjectMetadata(ctx, bucketName, objectName interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "GetObjectMetadata", reflect.TypeOf((*MockGCS)(nil).GetObjectMetadata), ctx, bucketName, objectName)
}

// SignURL mocks base method.
func (m *MockGCS) SignURL(bucketName, objectName, customHost string) (string, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "SignURL", bucketName, objectName, customHost)
	ret0, _ := ret[0].(string)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// SignURL indicates an expected call of SignURL.
func (mr *MockGCSMockRecorder) SignURL(bucketName, objectName, customHost interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "SignURL", reflect.TypeOf((*MockGCS)(nil).SignURL), bucketName, objectName, customHost)
}

// UpdateObjectMetadata mocks base method.
func (m *MockGCS) UpdateObjectMetadata(ctx context.Context, bucketName, objectName string, metadata map[string]string) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "UpdateObjectMetadata", ctx, bucketName, objectName, metadata)
	ret0, _ := ret[0].(error)
	return ret0
}

// UpdateObjectMetadata indicates an expected call of UpdateObjectMetadata.
func (mr *MockGCSMockRecorder) UpdateObjectMetadata(ctx, bucketName, objectName, metadata interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "UpdateObjectMetadata", reflect.TypeOf((*MockGCS)(nil).UpdateObjectMetadata), ctx, bucketName, objectName, metadata)
}

// UploadObject mocks base method.
func (m *MockGCS) UploadObject(ctx context.Context, bucketName, objectName, srcFilePath string) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "UploadObject", ctx, bucketName, objectName, srcFilePath)
	ret0, _ := ret[0].(error)
	return ret0
}

// UploadObject indicates an expected call of UploadObject.
func (mr *MockGCSMockRecorder) UploadObject(ctx, bucketName, objectName, srcFilePath interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "UploadObject", reflect.TypeOf((*MockGCS)(nil).UploadObject), ctx, bucketName, objectName, srcFilePath)
}
