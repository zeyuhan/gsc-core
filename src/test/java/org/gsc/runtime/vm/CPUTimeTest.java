package org.gsc.runtime.vm;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.application.GSCApplicationContext;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.gsc.runtime.GVMTestResult;
import org.gsc.runtime.GVMTestUtils;
import org.gsc.runtime.vm.program.Program.OutOfResourceException;
import org.gsc.common.storage.DepositImpl;
import org.gsc.common.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ReceiptCheckErrException;
import org.gsc.core.exception.TransactionTraceException;
import org.gsc.protos.Protocol.AccountType;

@Slf4j
@Ignore
public class CPUTimeTest {

  private Manager dbManager;
  private GSCApplicationContext context;
  private DepositImpl deposit;
  private String dbPath = "output_CPUTimeTest";
  private String OWNER_ADDRESS;


  /**
   * Init data.
   */
  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    deposit.addBalance(Hex.decode(OWNER_ADDRESS), 30000000000000L);
  }

  // solidity for endlessLoopTest
  // pragma solidity ^0.4.0;
  //
  // contract TestForEndlessLoop {
  //
  //   uint256 vote;
  //   constructor () public {
  //     vote = 0;
  //   }
  //
  //   function getVote() public constant returns (uint256 _vote) {
  //     _vote = vote;
  //   }
  //
  //   function setVote(uint256 _vote) public {
  //     vote = _vote;
  //     while(true)
  //     {
  //       vote += 1;
  //     }
  //   }
  // }

  @Test
  public void endlessLoopTest()
      throws ContractExeException, TransactionTraceException, ContractValidateException, ReceiptCheckErrException {

    long value = 0;
    long feeLimit = 20000000000000L;
    long consumeUserResourcePercent = 0;
    GVMTestResult result = deployEndlessLoopContract(value, feeLimit,
        consumeUserResourcePercent);
    Assert.assertEquals(result.getReceipt().getEnergyUsage(), 0);
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 5107);
    Assert.assertEquals(result.getReceipt().getOriginEnergyUsage(), 0);

    byte[] contractAddress = result.getContractAddress();

    /* =================================== CALL setVote(uint256) =================================== */
    String params = "0000000000000000000000000000000000000000000000000000000000000003";
    byte[] triggerData = GVMTestUtils.parseABI("setVote(uint256)", params);
    boolean haveException = false;
    result = GVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS), contractAddress,
            triggerData, value, feeLimit, deposit, null);
    Exception exception = result.getRuntime().getResult().getException();
    Assert.assertTrue(exception instanceof OutOfResourceException);
  }

  public GVMTestResult deployEndlessLoopContract(long value, long feeLimit,
                                                 long consumeUserResourcePercent)
      throws ContractExeException, ReceiptCheckErrException, TransactionTraceException, ContractValidateException {
    String contractName = "EndlessLoopContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":true,\"inputs\":[],\"name\":\"getVote\",\"outputs\":[{\"name\":\"_vote\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_vote\",\"type\":\"uint256\"}],\"name\":\"setVote\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    String code = "608060405234801561001057600080fd5b506000808190555060fa806100266000396000f3006080604052600436106049576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680630242f35114604e578063230796ae146076575b600080fd5b348015605957600080fd5b50606060a0565b6040518082815260200191505060405180910390f35b348015608157600080fd5b50609e6004803603810190808035906020019092919050505060a9565b005b60008054905090565b806000819055505b60011560cb576001600080828254019250508190555060b1565b505600a165627a7a72305820290a38c9bbafccaf6c7f752ab56d229e354da767efb72715ee9fdb653b9f4b6c0029";
    String libraryAddressPair = null;

    return GVMTestUtils
        .deployContractAndReturnTVMTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            deposit, null);
  }

  /**
   * Release resources.
   */
  @After
  public void destroy() {
    Args.clearParam();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
    context.destroy();
  }
}
