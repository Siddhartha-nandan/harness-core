package software.wings.service.intfc.yaml;

import org.eclipse.jgit.api.PullResult;
import software.wings.beans.GitConfig;
import software.wings.beans.yaml.GitCheckoutResult;
import software.wings.beans.yaml.GitCloneResult;
import software.wings.beans.yaml.GitCommitAndPushResult;
import software.wings.beans.yaml.GitCommitRequest;
import software.wings.beans.yaml.GitCommitResult;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitPushResult;

/**
 * Created by anubhaw on 10/16/17.
 */

/**
 * The interface Git client.
 */
public interface GitClient {
  /**
   * Clone git clone result.
   *
   * @param gitConfig  the git config
   * @return the git clone result
   */
  GitCloneResult clone(GitConfig gitConfig);

  /**
   * Diff git diff result.
   *
   * @param gitConfig     the git config
   * @param startCommitId the start commit id
   * @return the git diff result
   */
  GitDiffResult diff(GitConfig gitConfig, String startCommitId);

  /**
   * Checkout git checkout result.
   *
   * @param gitConfig the git config
   * @return the git checkout result
   */
  GitCheckoutResult checkout(GitConfig gitConfig);

  /**
   * Commit git commit result.
   *
   * @param gitConfig        the git config
   * @param gitCommitRequest the git commit request
   * @return the git commit result
   */
  GitCommitResult commit(GitConfig gitConfig, GitCommitRequest gitCommitRequest);

  /**
   * Push git push result.
   *
   * @param gitConfig the git config
   * @return the git push result
   */
  GitPushResult push(GitConfig gitConfig);

  /**
   * Commit and push git commit and push result.
   *
   * @param gitConfig        the git config
   * @param gitCommitRequest the git commit request
   * @return the git commit and push result
   */
  GitCommitAndPushResult commitAndPush(GitConfig gitConfig, GitCommitRequest gitCommitRequest);

  /**
   * Pull git pull result.
   *
   * @param gitConfig the git config
   * @return the git pull result
   */
  PullResult pull(GitConfig gitConfig);
}
