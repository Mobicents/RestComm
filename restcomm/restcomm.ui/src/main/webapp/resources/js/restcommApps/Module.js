angular.module("rcApp.restcommApps", ['ui.router']).config([ '$stateProvider',  function($stateProvider) {

	$stateProvider.state('restcomm.ras',{
	    url:'/ras',
	    templateUrl: 'modules/rappmanager.html',
        controller: 'RappManagerCtrl',
        resolve: {
            products: rappManagerCtrl.getProducts,
            localApps: function (rappService) { return rappService.refreshLocalApps();}
        },
        parent:'restcomm'
	});
	$stateProvider.state('restcomm.ras-config',{
	    url:'/ras/config/:applicationSid=:projectName/:mode?',
		templateUrl: 'modules/rappmanager-config.html',
		controller: 'RappManagerConfigCtrl',
		resolve: {
			rappConfig : function (rappService, $route) { return rappService.getAppConfig($route.current.params.applicationSid);}, //, $route.current.params.mode); },
			rapp: function (rappService, $route) {return rappService.getApp($route.current.params.applicationSid);},
			bootstrapObject : function (rappService, $route) { return rappService.getBoostrapObject($route.current.params.applicationSid); }
		},
		parent:'restcomm'
	});

	/*
	$routeProvider
	.when('/ras', {
		templateUrl: 'modules/rappmanager.html', 
		controller: 'RappManagerCtrl', 
		resolve: {
			products: rappManagerCtrl.getProducts, 
			localApps: function (rappService) { return rappService.refreshLocalApps();}
		} 
	})
	.when('/ras/config/:applicationSid=:projectName/:mode?', {
		templateUrl: 'modules/rappmanager-config.html', 
		controller: 'RappManagerConfigCtrl', 
		resolve: { 
			rappConfig : function (rappService, $route) { return rappService.getAppConfig($route.current.params.applicationSid);}, //, $route.current.params.mode); },
			rapp: function (rappService, $route) {return rappService.getApp($route.current.params.applicationSid);},
			bootstrapObject : function (rappService, $route) { return rappService.getBoostrapObject($route.current.params.applicationSid); }
		}
	});
	*/
}]);

	

