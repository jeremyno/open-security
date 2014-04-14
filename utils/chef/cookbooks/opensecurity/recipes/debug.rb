package "visualvm" do
  action :install
end

bash "Add jmx option" do
  not_if "grep 'jmxremote.port' #{node[:opensecurity][:tomcat][:defaultConfig]}"
  code <<-EOH
     echo 'export CATALINA_OPTS="$CATALINA_OPTS -Dcom.sun.management.jmxremote.port=8086 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"' >> #{node[:opensecurity][:tomcat][:defaultConfig]}
  EOH
end

service node[:opensecurity][:tomcat][:service] do
  action [:restart]
end
