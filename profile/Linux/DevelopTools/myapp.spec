Name:		myapp
Version:	1.0
Release:	1%{?dist}
Summary:	Trivial application

Group:		Applications/Media
License:	Copyright 2020 God Publishing, Inc.
URL:		
Source0:	%{name}-%{version}.tar.gz
Buildroot:	%{_tmppath}/%{name}-%{version}-root

Provides:	goodness
BuildRequires:	
Requires:	

%description
Myapp Trivial Application
A trivial application used to demonstrate development tools.


%prep
%setup -q


%build
%configure
make %{?_smp_mflags}


%install
mkdir -p $RPM_BUILD_ROOT%{_bindir}
mkdir -p $RPM_BUILD_ROOT%{_mandir}
install -m755 myapp $RPM_BUILD_ROOT%{_bindir}/myapp
install -m755 myapp.1 $RPM_BUILD_ROOT%{_mandir}/myapp.1
#%make_install


%clean
rm -rf $RPM_BUILD_ROOT

%files
%doc
%{_bindir}/myapp
%{_mandir}/myapp.1


%post
mail root -s "myapp installed - please see see" < /dev/null


%changelog

