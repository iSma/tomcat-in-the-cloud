package org.example.kubeping.membership;

import org.apache.catalina.tribes.Member;

import java.util.List;
import java.util.Properties;

public interface MemberProvider {
    void init(Properties properties) throws Exception;

    List<? extends Member> getMembers() throws Exception;
}
