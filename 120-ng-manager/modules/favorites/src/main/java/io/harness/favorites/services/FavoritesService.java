/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.favorites.services;

import io.harness.exception.InvalidRequestException;
import io.harness.favorites.ResourceType;
import io.harness.favorites.entities.Favorite;
import io.harness.spec.server.ng.v1.model.FavoriteDTO;

import java.util.List;

public interface FavoritesService {
  /**
   *
   * @param favoriteDTO
   * @return FavoriteEntity
   */
  Favorite createFavorite(FavoriteDTO favoriteDTO, String accountIdentifier);

  /**
   *
   * @param accountIdentifier
   * @param orgIdentifier
   * @param projectIdentifier
   * @param userId
   * @param resourceType
   * @return a list of favorite present in the scope for the matching resource type of the user
   */

  List<Favorite> getFavorites(String accountIdentifier, String orgIdentifier, String projectIdentifier, String userId,
      ResourceType resourceType);

  /**
   *
   * @param accountIdentifier
   * @param orgIdentifier
   * @param projectIdentifier
   * @param userId
   * @return a list of favorites present in the scope for the user
   */

  List<Favorite> getFavorites(String accountIdentifier, String orgIdentifier, String projectIdentifier, String userId);

  /**
   *
   * @param accountIdentifier
   * @param orgIdentifier
   * @param projectIdentifier
   * @param userId
   * @param resourceType
   * @param resourceId
   */

  void deleteFavorite(String accountIdentifier, String orgIdentifier, String projectIdentifier, String userId,
      String resourceType, String resourceId) throws InvalidRequestException;
}
