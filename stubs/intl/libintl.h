/* Minimal no-op libintl for Android (no translations). Written for this project. */
#ifndef _LIBINTL_H
#define _LIBINTL_H 1
#include <locale.h>
#ifdef __cplusplus
extern "C" {
#endif
char *gettext(const char *msgid) __attribute__((format_arg(1)));
char *dgettext(const char *domain, const char *msgid) __attribute__((format_arg(2)));
char *dcgettext(const char *domain, const char *msgid, int category) __attribute__((format_arg(2)));
char *ngettext(const char *msgid1, const char *msgid2, unsigned long n) __attribute__((format_arg(1))) __attribute__((format_arg(2)));
char *dngettext(const char *domain, const char *msgid1, const char *msgid2, unsigned long n) __attribute__((format_arg(2))) __attribute__((format_arg(3)));
char *dcngettext(const char *domain, const char *msgid1, const char *msgid2, unsigned long n, int category) __attribute__((format_arg(2))) __attribute__((format_arg(3)));
char *textdomain(const char *domain);
char *bindtextdomain(const char *domain, const char *dirname);
char *bind_textdomain_codeset(const char *domain, const char *codeset);
#ifdef __cplusplus
}
#endif
#endif
