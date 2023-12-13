// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package handler

import (
	"encoding/json"
	"net/http"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

const (
	accountIDParam = "accountID"
	keyParam       = "key"
	snapshotParam  = "snapshot"
	usePrefixParam = "prefix"
	keyListParam   = "keyList"
)

// writeBadRequest writes the json-encoded error message
// to the response with a 400 bad request status code.
func WriteBadRequest(w http.ResponseWriter, err error) {
	writeError(w, err, 400)
}

// writeNotFound writes the json-encoded error message to
// the response with a 404 not found status code.
func WriteNotFound(w http.ResponseWriter, err error) {
	writeError(w, err, 404)
}

// writeInternalError writes the json-encoded error message
// to the response with a 500 internal server error.
func WriteInternalError(w http.ResponseWriter, err error) {
	writeError(w, err, 500)
}

func CreateAccountSeparatedKey(accountID string, key string) string {
	return accountID + "/" + key
}

// writeJSON writes the json-encoded representation of v to
// the response body.
func WriteJSON(w http.ResponseWriter, v interface{}, status int) {
	for k, v := range noCacheHeaders {
		w.Header().Set(k, v)
	}
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	enc := json.NewEncoder(w)
	enc.SetIndent("", "  ")
	enc.Encode(v)
}

func WriteUnescapeJSON(w http.ResponseWriter, v interface{}, status int) {
	for k, v := range noCacheHeaders {
		w.Header().Set(k, v)
	}
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	enc := json.NewEncoder(w)
	enc.SetIndent("", "  ")
	enc.SetEscapeHTML(false)
	enc.Encode(v)
}

// writeError writes the json-encoded error message to the
// response.
func writeError(w http.ResponseWriter, err error, status int) {
	out := struct {
		Message string `json:"error_msg"`
	}{err.Error()}
	WriteJSON(w, &out, status)
}

type harnessClaims struct {
	Type     string `json:"type"`
	Name     string `json:"name"`
	Email    string `json:"email"`
	Username string `json:"username"`
	jwt.RegisteredClaims
}

func GenerateJWTToken(jwtSecret string) (string, error) {
	var (
		tokenTypeService = "SERVICE"
		tokenIssuer      = "Harness Inc"
	)

	// Valid from an hour ago
	issuedTime := jwt.NewNumericDate(time.Now().Add(-time.Hour))

	// Expires in an hour from now
	expiryTime := jwt.NewNumericDate(time.Now().Add(time.Hour))

	harnessClaims := harnessClaims{
		Type: tokenTypeService,
		Name: "LOG_SVC",
	}

	harnessClaims.Issuer = tokenIssuer
	harnessClaims.IssuedAt = issuedTime
	harnessClaims.NotBefore = issuedTime
	harnessClaims.ExpiresAt = expiryTime

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, harnessClaims)
	signedJwt, err := token.SignedString([]byte(jwtSecret))
	if err != nil {
		return "", err
	}

	return signedJwt, nil
}
