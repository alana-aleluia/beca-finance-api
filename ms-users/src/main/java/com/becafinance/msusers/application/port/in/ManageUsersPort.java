package com.becafinance.msusers.application.port.in;

import com.becafinance.msusers.domain.model.User;
import java.util.List;

public interface ManageUsersPort {
    List<User> list();
    User find(Long id);
    User update(Long id, String name, String email);
    void delete(Long id);
}
