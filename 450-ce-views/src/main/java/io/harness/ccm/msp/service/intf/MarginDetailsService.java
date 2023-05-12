package io.harness.ccm.msp.service.intf;

import io.harness.ccm.msp.entities.ManagedAccountDetails;
import io.harness.ccm.msp.entities.ManagedAccountTimeSeriesData;
import io.harness.ccm.msp.entities.ManagedAccountsOverview;
import io.harness.ccm.msp.entities.MarginDetails;

import java.util.List;

public interface MarginDetailsService {
  String save(MarginDetails marginDetails);
  String addManagedAccount(String mspAccountId, String managedAccountId, String managedAccountName);
  MarginDetails update(MarginDetails marginDetails);
  MarginDetails unsetMargins(String uuid, String accountId);
  MarginDetails get(String uuid);
  MarginDetails get(String mspAccountId, String managedAccountId);
  List<MarginDetails> list(String mspAccountId);
  List<ManagedAccountDetails> listManagedAccountDetails(String mspAccountId);
  ManagedAccountsOverview getTotalMarkupAndSpend(String mspAccountId);
  ManagedAccountsOverview getTotalMarkupAndSpend(String mspAccountId, String managedAccountId);
  ManagedAccountTimeSeriesData getManagedAccountTimeSeriesData(String managedAccountId);
}
