package com.becafinance.mstransactions.application.port.out;
import com.becafinance.mstransactions.application.dto.AccountData;
public interface AccountDataPort { AccountData load(long userId); }
