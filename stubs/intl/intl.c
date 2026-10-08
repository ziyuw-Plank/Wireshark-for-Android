#include "libintl.h"
char *gettext(const char *m){return (char*)m;}
char *dgettext(const char *d,const char *m){(void)d;return (char*)m;}
char *dcgettext(const char *d,const char *m,int c){(void)d;(void)c;return (char*)m;}
char *ngettext(const char *a,const char *b,unsigned long n){return (char*)(n==1?a:b);}
char *dngettext(const char *d,const char *a,const char *b,unsigned long n){(void)d;return (char*)(n==1?a:b);}
char *dcngettext(const char *d,const char *a,const char *b,unsigned long n,int c){(void)d;(void)c;return (char*)(n==1?a:b);}
char *textdomain(const char *d){return (char*)(d?d:"messages");}
char *bindtextdomain(const char *d,const char *dir){(void)d;return (char*)(dir?dir:"/");}
char *bind_textdomain_codeset(const char *d,const char *c){(void)d;return (char*)c;}
