# HAT Deployment using Ansible


## Installing Ansible

Make sure Ansible is installed on your local machine. The latest version 
at the time of writing is 1.9.1. The simplest way to install Ansible 
is using **pip**, the Python package manager.

```
$ sudo easy_install pip
$ sudo pip install ansible
```

For more details, see the Ansible installation instructions 
[here](http://docs.ansible.com/intro_installation.html#latest-releases-via-pip).


## Using Ansible to install the application on the deployment server

The following command assumes that SSH authentication (password free login) 
has been setup for user `dsingh` on the deployment server, and that 
installing software (like Apache web server) requires SUDO password 
authentication (same as the user password on Ubuntu). 

The following command will ask for the SUDO password on the remote server, 
then install the application on the remote server.
```
$ ansible-playbook deploy.yml
```

To restart the nginx webserver and nodejs apps only, you could do:
```
$ ansible-playbook deploy.yml --tags restart
```

To restart just the nginx webserver or node apps, do one of:
```
$ ansible-playbook deploy.yml --tags restart-nginx
$ ansible-playbook deploy.yml --tags restart-node 
```
To upload files to the remote server only:
```
$ ansible-playbook deploy.yml --tags upload
```

Finally, to upload all files and restart nginx and nodejs, do:
```
$ ansible-playbook deploy.yml --tags "upload,restart"
```


Th script `deploy.yml` was developed using the great tutorial
[here](https://www.digitalocean.com/community/tutorials/how-to-configure-apache-using-ansible-on-ubuntu-14-04).
