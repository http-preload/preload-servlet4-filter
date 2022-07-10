function getUserAgentData(userAgent, uaBrands, uaMobile, uaPlatform) {
  var mobile, platform = '', brands = [];
  var notABrand = {brand: '.Not/A)Brand', version: '99.0.0.0'};
  if (uaBrands) {
    (uaBrands || '').replace(/"([^"]+)";v="(\d+)"(, |$)/g, function(_, $1, $2) {
      brands.push({brand: $1, version: $2});
      return '';
    });
    if (brands.length === 0) {
      brands.push(notABrand);
    }
    mobile = uaMobile === '?1';
    platform = uaPlatform ? uaPlatform.slice(1, -2) : 'Unknown';
    return {brands:brands, mobile:mobile, platform:platform};
  }
  var fullVersionList = [];
  var platformInfo = userAgent;
  var found = false;
  var versionInfo = userAgent.replace(/\(([^)]+)\)?/g, function(_, $1) {
    if (!found) {
      platformInfo = $1;
      found = true;
    }
    return '';
  });
  // detect mobile
  mobile = userAgent.indexOf('Mobile') !== -1;
  var m;
  var m2;
  // detect platform
  if ((m = /Windows NT (\d+(\.\d+)*)/.exec(platformInfo)) !== null) {
    platform = 'Windows';
  } else if ((m = /Android (\d+(\.\d+)*)/.exec(platformInfo)) !== null) {
    platform = 'Android';
  } else if ((m = /(iPhone|iPod touch); CPU iPhone OS (\d+(_\d+)*)/.exec(platformInfo)) !== null) {
    // see special notes at https://www.whatismybrowser.com/guides/the-latest-user-agent/safari
    platform = 'iOS';
  } else if ((m = /(iPad); CPU OS (\d+(_\d+)*)/.exec(platformInfo)) !== null) {
    platform = 'iOS';
  } else if ((m = /Macintosh; (Intel|\w+) Mac OS X (\d+(_\d+)*)/.exec(platformInfo)) !== null) {
    platform = 'macOS';
  } else if ((m = /Linux/.exec(platformInfo)) !== null) {
    platform = 'Linux';
  } else if ((m = /CrOS (\w+) (\d+(\.\d+)*)/.exec(platformInfo)) !== null) {
    platform = 'Chrome OS';
  }
  if (!platform) {
    platform = 'Unknown';
  }
  // detect fullVersionList / brands
  fullVersionList.push(notABrand);
  if ((m = /Chrome\/(\d+(\.\d+)*)/.exec(versionInfo)) !== null) {
    if ((m2 = /(Edge?)\/(\d+(\.\d+)*)/.exec(versionInfo)) !== null) {
      var identBrandMap = {
        'Edge': 'Microsoft Edge',
        'Edg': 'Microsoft Edge',
      };
      if (parseInt(m[2]) >= 79) {
        fullVersionList.push({brand: 'Chromium', version: m[2]});
      }
      var brand = identBrandMap[m[1]];
      fullVersionList.push({brand: brand, version: m2[2]});
    } else {
      fullVersionList.push({brand: 'Chromium', version: m[1]});
      fullVersionList.push({brand: 'Google Chrome', version: m[1]});
    }
  } else if ((m = /AppleWebKit\/(\d+(\.\d+)*)/.exec(versionInfo)) !== null) {
    fullVersionList.push({brand: 'WebKit', version: m[1]});
    if (platform === 'iOS') {
      let safariVer;
      if ((m2 = /Version\/(\d+(\.\d+)*)/.exec(versionInfo)) != null) {
        safariVer = m2[1];
        fullVersionList.push({brand: 'Safari', version: m2[1]});
      } else {
        safariVer = '0';
        fullVersionList.push({brand: 'Safari', version: '0'});
      }
      if ((m2 = /(CriOS|EdgiOS|FxiOS)\/(\d+(\.\d+)*)/.exec(versionInfo)) != null) {
        var identBrandMap = {
          'CriOS': 'Google Chrome',
          'EdgiOS': 'Microsoft Edge',
          'FxiOS': 'Mozilla Firefox'
        };
        var brand = identBrandMap[m2[1]];
        fullVersionList.push({brand:brand, version: m2[2]});
      }else{
        fullVersionList.push({brand: 'Apple Safari', version: safariVer});
      }
    }else{
      if ((m2 = /Version\/(\d+(\.\d+)*)/.exec(versionInfo)) != null) {
        fullVersionList.push({brand: 'Safari', version: m2[1]});
        fullVersionList.push({brand: 'Apple Safari', version: m2[1]});
      }
    }
  } else if ((m = /Firefox\/(\d+(\.\d+)*)/.exec(versionInfo)) !== null) {
    fullVersionList.push({brand: 'Firefox', version: m[1]});
    fullVersionList.push({brand: 'Mozilla Firefox', version: m[1]});
  } else if ((m = /(MSIE |rv:)(\d+\.\d+)/.exec(platformInfo)) !== null) {
    fullVersionList.push({brand: 'Internet Explorer', version: m[2]});
  }
  brands = fullVersionList.map(function(b) {
    var pos = b.version.indexOf('.');
    var version = pos === -1 ? b.version : b.version.slice(0, pos);
    return {brand: b.brand, version:version};
  });
  return {brands:brands, mobile:mobile, platform:platform};
}