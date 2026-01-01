Pod::Spec.new do |spec|
    spec.name                     = 'kmpauth_facebook'
    spec.version                  = '2.5.0-alpha01'
    spec.homepage                 = ''
    spec.source                   = { :http=> ''}
    spec.authors                  = ''
    spec.license                  = ''
    spec.summary                  = ''
    spec.vendored_frameworks      = 'build/cocoapods/framework/KMPAuthFacebook.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target    = '12.0'
    spec.dependency 'FBSDKCoreKit', '18.0.0'
    spec.dependency 'FBSDKLoginKit', '18.0.0'
                
    if !Dir.exist?('build/cocoapods/framework/KMPAuthFacebook.framework') || Dir.empty?('build/cocoapods/framework/KMPAuthFacebook.framework')
        raise "

        Kotlin framework 'KMPAuthFacebook' doesn't exist yet, so a proper Xcode project can't be generated.
        'pod install' should be executed after running ':generateDummyFramework' Gradle task:

            ./gradlew :kmpauth-facebook:generateDummyFramework

        Alternatively, proper pod installation is performed during Gradle sync in the IDE (if Podfile location is set)"
    end
                
    spec.xcconfig = {
        'ENABLE_USER_SCRIPT_SANDBOXING' => 'NO',
    }
                
    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':kmpauth-facebook',
        'PRODUCT_MODULE_NAME' => 'KMPAuthFacebook',
    }
                
    spec.script_phases = [
        {
            :name => 'Build kmpauth_facebook',
            :execution_position => :before_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                  exit 0
                fi
                set -ev
                REPO_ROOT="$PODS_TARGET_SRCROOT"
                "$REPO_ROOT/../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:syncFramework \
                    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
                    -Pkotlin.native.cocoapods.archs="$ARCHS" \
                    -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
            SCRIPT
        }
    ]
    spec.resources = ['build/compose/cocoapods/compose-resources']
end