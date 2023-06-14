// Copyright 2023 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package utils

import (
	"bytes"
	"encoding/json"
)

// VoteType defines whether user liked/disliked a suggestion.
type VoteType int

// VoteType enumeration.
const (
	Unknown VoteType = iota
	Upvote
	Downvote
)

func (s VoteType) String() string {
	return voteTypeID[s]
}

var voteTypeID = map[VoteType]string{
	Unknown:  "Unknown",
	Upvote:   "Upvote",
	Downvote: "Downvote",
}

var voteTypeName = map[string]VoteType{
	"":         Unknown,
	"Unknown":  Unknown,
	"Upvote":   Upvote,
	"Downvote": Downvote,
}

// MarshalJSON marshals the string representation of the
// vote type to JSON.
func (s *VoteType) MarshalJSON() ([]byte, error) {
	buffer := bytes.NewBufferString(`"`)
	buffer.WriteString(voteTypeID[*s])
	buffer.WriteString(`"`)
	return buffer.Bytes(), nil
}

// UnmarshalJSON unmarshals the json representation of the
// vote type from a string value.
func (s *VoteType) UnmarshalJSON(b []byte) error {
	// unmarshal as string
	var a string
	err := json.Unmarshal(b, &a)
	if err != nil {
		return err
	}
	// lookup value
	*s = voteTypeName[a]
	return nil
}
