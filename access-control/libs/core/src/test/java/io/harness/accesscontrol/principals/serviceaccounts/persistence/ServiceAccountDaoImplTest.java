package io.harness.accesscontrol.principals.serviceaccounts.persistence;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.KARAN;

import static junit.framework.TestCase.assertEquals;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.accesscontrol.AccessControlCoreTestBase;
import io.harness.accesscontrol.principals.serviceaccounts.ServiceAccount;
import io.harness.accesscontrol.principals.serviceaccounts.persistence.ServiceAccountDBO.ServiceAccountDBOKeys;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.rule.Owner;
import io.harness.utils.PageUtils;

import com.google.common.collect.Lists;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public class ServiceAccountDaoImplTest extends AccessControlCoreTestBase {
  private ServiceAccountRepository serviceAccountRepository;
  private ServiceAccountDao serviceAccountDao;

  @Before
  public void setup() {
    serviceAccountRepository = mock(ServiceAccountRepository.class);
    serviceAccountDao = spy(new ServiceAccountDaoImpl(serviceAccountRepository));
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreateIfNotPresentAlreadyPresent() {
    ServiceAccount serviceAccount = ServiceAccount.builder().build();
    ServiceAccountDBO serviceAccountDBO = ServiceAccountDBO.builder().build();
    when(serviceAccountRepository.findByIdentifierAndScopeIdentifier(
             serviceAccount.getIdentifier(), serviceAccount.getScopeIdentifier()))
        .thenReturn(Optional.of(serviceAccountDBO));
    assertEquals(serviceAccount, serviceAccountDao.createIfNotPresent(serviceAccount));
    verify(serviceAccountRepository, times(1))
        .findByIdentifierAndScopeIdentifier(serviceAccount.getIdentifier(), serviceAccount.getScopeIdentifier());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreateIfNotPresentNotPresent() {
    ServiceAccount serviceAccount = ServiceAccount.builder().build();
    ServiceAccountDBO serviceAccountDBO = ServiceAccountDBO.builder().build();
    when(serviceAccountRepository.findByIdentifierAndScopeIdentifier(
             serviceAccount.getIdentifier(), serviceAccount.getScopeIdentifier()))
        .thenReturn(Optional.empty());
    when(serviceAccountRepository.save(serviceAccountDBO)).thenReturn(serviceAccountDBO);
    assertEquals(serviceAccount, serviceAccountDao.createIfNotPresent(serviceAccount));
    verify(serviceAccountRepository, times(1))
        .findByIdentifierAndScopeIdentifier(serviceAccount.getIdentifier(), serviceAccount.getScopeIdentifier());
    verify(serviceAccountRepository, times(1)).save(serviceAccountDBO);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testList() {
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(1).build();
    Pageable pageable = PageUtils.getPageRequest(pageRequest);
    String scopeIdentifier = randomAlphabetic(10);
    PageResponse pageResponse = PageUtils.getNGPageResponse(Page.empty());
    when(serviceAccountRepository.findByScopeIdentifier(scopeIdentifier, pageable)).thenReturn(Page.empty());
    assertEquals(pageResponse, serviceAccountDao.list(pageRequest, scopeIdentifier));
    verify(serviceAccountRepository, times(1)).findByScopeIdentifier(scopeIdentifier, pageable);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testGet() {
    String identifier = randomAlphabetic(10);
    String scopeIdentifier = randomAlphabetic(10);
    Optional<ServiceAccountDBO> serviceAccountDBOOptional = Optional.of(ServiceAccountDBO.builder().build());
    when(serviceAccountRepository.findByIdentifierAndScopeIdentifier(identifier, scopeIdentifier))
        .thenReturn(serviceAccountDBOOptional);
    assertEquals(Optional.of(ServiceAccount.builder().build()), serviceAccountDao.get(identifier, scopeIdentifier));
    verify(serviceAccountRepository, times(1)).findByIdentifierAndScopeIdentifier(identifier, scopeIdentifier);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testGetNotFound() {
    String identifier = randomAlphabetic(10);
    String scopeIdentifier = randomAlphabetic(10);
    Optional<ServiceAccountDBO> serviceAccountDBOOptional = Optional.empty();
    when(serviceAccountRepository.findByIdentifierAndScopeIdentifier(identifier, scopeIdentifier))
        .thenReturn(serviceAccountDBOOptional);
    assertEquals(Optional.empty(), serviceAccountDao.get(identifier, scopeIdentifier));
    verify(serviceAccountRepository, times(1)).findByIdentifierAndScopeIdentifier(identifier, scopeIdentifier);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testDelete() {
    String identifier = randomAlphabetic(10);
    String scopeIdentifier = randomAlphabetic(10);
    ServiceAccountDBO serviceAccountDBO = ServiceAccountDBO.builder().build();
    when(serviceAccountRepository.deleteByIdentifierAndScopeIdentifier(identifier, scopeIdentifier))
        .thenReturn(Lists.newArrayList(serviceAccountDBO));
    assertEquals(Optional.of(ServiceAccount.builder().build()), serviceAccountDao.delete(identifier, scopeIdentifier));
    verify(serviceAccountRepository, times(1)).deleteByIdentifierAndScopeIdentifier(identifier, scopeIdentifier);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testDeleteNotFound() {
    String identifier = randomAlphabetic(10);
    String scopeIdentifier = randomAlphabetic(10);
    when(serviceAccountRepository.deleteByIdentifierAndScopeIdentifier(identifier, scopeIdentifier))
        .thenReturn(Lists.newArrayList());
    assertEquals(Optional.empty(), serviceAccountDao.delete(identifier, scopeIdentifier));
    verify(serviceAccountRepository, times(1)).deleteByIdentifierAndScopeIdentifier(identifier, scopeIdentifier);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testDeleteInScopesAndChildScopes() {
    String identifier = randomAlphabetic(10);
    String scopeIdentifier = randomAlphabetic(10);
    Criteria criteria = Criteria.where(ServiceAccountDBOKeys.identifier).is(identifier);
    Pattern startsWithScope = Pattern.compile("^".concat(scopeIdentifier));
    criteria.and(ServiceAccountDBOKeys.scopeIdentifier).regex(startsWithScope);
    when(serviceAccountRepository.deleteMulti(criteria)).thenReturn(10L);
    assertEquals(10L, serviceAccountDao.deleteInScopesAndChildScopes(identifier, scopeIdentifier));
    verify(serviceAccountRepository, times(1)).deleteMulti(criteria);
  }
}
