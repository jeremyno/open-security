#
# Cookbook Name:: opensecurity

# Recipe:: default
#
# Copyright 2013, YOUR_COMPANY_NAME
#
# All rights reserved - Do Not Redistribute
#

execute "update" do
  command "apt-get update -f -y"
end

["openjdk-7-jdk",node[:opensecurity][:tomcat][:package],"curl"].each do |pkg|
  package pkg do
    action :install
  end
end

execute "get webapp" do
  command "curl -o #{Chef::Config[:file_cache_path]}/opensecurity.war #{node[:opensecurity][:warsource]}"
end

service node[:opensecurity][:tomcat][:service] do
  action :stop
end

execute "remove root" do
  command "rm -rf #{node[:opensecurity][:tomcat][:webappDir]}/ROOT*";
end

execute "copy application" do
  command "cp #{Chef::Config[:file_cache_path]}/opensecurity.war #{node[:opensecurity][:tomcat][:webappDir]}/ROOT.war";
end

bash "Add config option" do
  not_if "grep 'opencam.configPath' #{node[:opensecurity][:tomcat][:defaultConfig]}"
  code <<-EOH
     echo 'JAVA_OPTS="$JAVA_OPTS -Dopencam.configPath=#{node[:opensecurity][:configPath]}"' >> #{node[:opensecurity][:tomcat][:defaultConfig]}
  EOH
end

directory node[:opensecurity][:configDir] do
  owner node[:opensecurity][:tomcat][:user]
  owner node[:opensecurity][:tomcat][:group]
  mode 0700
  action :create
end

directory node[:opensecurity][:localcache] do
  owner node[:opensecurity][:tomcat][:user]
  owner node[:opensecurity][:tomcat][:group]
  mode 0700
  action :create
end

directory node[:opensecurity][:logdirectory] do
  owner node[:opensecurity][:tomcat][:user]
  owner node[:opensecurity][:tomcat][:group]
  mode 0755
  action :create
end

template "config" do
  path node[:opensecurity][:configPath]
  owner node[:opensecurity][:tomcat][:user]
  owner node[:opensecurity][:tomcat][:group]
  mode 0750
  action :create
end

template "tomcat-users.xml" do
  path node[:opensecurity][:tomcat][:userfile]
  owner node[:opensecurity][:tomcat][:user]
  owner node[:opensecurity][:tomcat][:group]
  mode 0750
  action :create
end


service node[:opensecurity][:tomcat][:service] do
  action [:start, :restart]
end
