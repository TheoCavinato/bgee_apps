'use strict';

/* Controllers */

var testApp = angular.module('angularBasicTest', []);

testApp.controller('PhoneListCtrl', function($scope) {
  $scope.phones = [
    {'name': 'Nexus S',
     'snippet': 'Fast just got faster with Nexus S.'},
    {'name': 'Motorola XOOM with Wi-Fi',
     'snippet': 'The Next, Next Generation tablet.'},
    {'name': 'MOTOROLA XOOM',
     'snippet': 'The Next, Next Generation tablet.'}
  ];
});

testApp.controller("ajaxTestCtrl", function($scope, $http) {
    $scope.myData = {};
    $scope.myData.doClick = function(item, event) {
        //get the URL to perform query
    	var urlGenerator = new requestParameters();
    	urlGenerator.setPage(urlGenerator.PAGE_TOP_ANAT());
    	//usually there would be other parameters, typically the 'action' parameter 
    	//(that can be set through the method setAction). 
    	//for now, we simply set the JSON 'home' page of top anat 
    	//to respond to this AJAX query. If you were removing the following line, 
    	//the server would respond with the complete HTML topAnat 'home' page.
    	urlGenerator.setDisplayType(urlGenerator.DISPLAY_TYPE_JSON());
    	//call getRequestURL with true, this adds a parameter allowing the server 
    	//to detect an AJAX query
    	var url = urlGenerator.getRequestURL(true);
    	
        var responsePromise = $http.get(url);

        responsePromise.success(function(data, status, headers, config) {
            $scope.myData.fromServer = data.title;
        });
        responsePromise.error(function(data, status, headers, config) {
        	$scope.myData.fromServer = "AJAX failed! " + status;
        });
    }


} );