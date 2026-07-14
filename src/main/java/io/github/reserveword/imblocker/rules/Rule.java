package io.github.reserveword.imblocker.rules;

public interface Rule {
   double priority();
   boolean apply();
}
