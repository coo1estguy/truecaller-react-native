import {
  AndroidConfig,
  ConfigPlugin,
  createRunOncePlugin,
  withAndroidManifest,
  withEntitlementsPlist,
  withInfoPlist,
  withPodfile,
} from "expo/config-plugins";

const pkg = require("../../package.json");

type TruecallerPluginConfig = {
  androidClientId?: string;
  iosAppKey?: string;
  iosAppLink?: string;
};

function validateProps(props: TruecallerPluginConfig): void {
  if (props.iosAppKey && !props.iosAppLink) {
    throw new Error(
      `truecaller-react-native: "iosAppLink" is required when "iosAppKey" is provided.`
    );
  }
  if (props.iosAppLink && !props.iosAppKey) {
    throw new Error(
      `truecaller-react-native: "iosAppKey" is required when "iosAppLink" is provided.`
    );
  }
}

const withTruecallerAndroid: ConfigPlugin<{ androidClientId: string }> = (
  config,
  { androidClientId }
) => {
  return withAndroidManifest(config, (config) => {
    const mainApp =
      AndroidConfig.Manifest.getMainApplicationOrThrow(config.modResults);

    if (mainApp["meta-data"]) {
      mainApp["meta-data"] = mainApp["meta-data"].filter(
        (item) =>
          item.$?.["android:name"] !== "com.truecaller.android.sdk.ClientId"
      );
    }

    AndroidConfig.Manifest.addMetaDataItemToMainApplication(
      mainApp,
      "com.truecaller.android.sdk.ClientId",
      androidClientId
    );

    return config;
  });
};

const withTruecallerIOSPlist: ConfigPlugin<{
  iosAppKey: string;
  iosAppLink: string;
}> = (config, { iosAppKey, iosAppLink }) => {
  return withInfoPlist(config, (config) => {
    const plist = config.modResults;

    const schemes: string[] = plist.LSApplicationQueriesSchemes ?? [];
    if (!schemes.includes("truesdk")) schemes.push("truesdk");
    plist.LSApplicationQueriesSchemes = schemes;

    const urlScheme = `truecallersdk-${iosAppKey}`;
    const urlTypes: Array<{
      CFBundleURLSchemes: string[];
      CFBundleURLName?: string;
    }> = plist.CFBundleURLTypes ?? [];
    if (!urlTypes.some((t) => t.CFBundleURLSchemes?.includes(urlScheme))) {
      urlTypes.push({ CFBundleURLSchemes: [urlScheme] });
    }
    plist.CFBundleURLTypes = urlTypes;

    plist["TruecallerAppKey"] = iosAppKey;
    plist["TruecallerAppLink"] = iosAppLink;

    return config;
  });
};

const withTruecallerIOSAssociatedDomains: ConfigPlugin<{
  iosAppLink: string;
}> = (config, { iosAppLink }) => {
  return withEntitlementsPlist(config, (config) => {
    let domain: string;
    try {
      domain = new URL(iosAppLink).hostname;
    } catch {
      domain = iosAppLink.replace(/^https?:\/\//, "").replace(/\/.*$/, "");
    }

    const domains: string[] =
      (config.modResults["com.apple.developer.associated-domains"] as
        | string[]
        | undefined) ?? [];

    const entry = `applinks:${domain}`;
    if (!domains.includes(entry)) domains.push(entry);
    config.modResults["com.apple.developer.associated-domains"] = domains;

    return config;
  });
};

const TRUECALLER_ASSETS_CAR_FIX_START =
  "# [truecaller-react-native] Fix TrueSDK Assets.car collision (start)";
const TRUECALLER_ASSETS_CAR_FIX_END =
  "# [truecaller-react-native] Fix TrueSDK Assets.car collision (end)";

const TRUECALLER_ASSETS_CAR_FIX = `${TRUECALLER_ASSETS_CAR_FIX_START}
assets_car = '\${TARGET_BUILD_DIR}/\${UNLOCALIZED_RESOURCES_FOLDER_PATH}/Assets.car'

installer.aggregate_targets.each do |aggregate_target|
  user_project = aggregate_target.user_project
  next unless user_project

  changed = false
  user_project.native_targets.each do |native_target|
    native_target.build_phases.each do |build_phase|
      next unless build_phase.respond_to?(:name) && build_phase.name == '[CP] Copy Pods Resources'
      next unless build_phase.respond_to?(:output_paths)

      original_output_paths = build_phase.output_paths.dup
      build_phase.output_paths = build_phase.output_paths.reject { |output_path| output_path == assets_car }
      changed ||= original_output_paths != build_phase.output_paths
    end
  end

  user_project.save if changed
end

Dir.glob(File.join(installer.sandbox.root, 'Target Support Files', 'Pods-*', '*resources-output-files.xcfilelist')).each do |filelist_path|
  lines = File.readlines(filelist_path)
  next_lines = lines.reject { |line| line.strip == assets_car }
  File.write(filelist_path, next_lines.join) if next_lines != lines
end
${TRUECALLER_ASSETS_CAR_FIX_END}`;

function removeTaggedBlock(
  contents: string,
  start: string,
  end: string
): string {
  const escapedStart = start.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const escapedEnd = end.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const existingBlock = new RegExp(
    `\\n?\\s*${escapedStart}[\\s\\S]*?\\s*${escapedEnd}\\n?`,
    "m"
  );

  return contents.replace(existingBlock, "\n");
}

function addTruecallerAssetsCarFix(podfile: string): string {
  const nextPodfile = removeTaggedBlock(
    podfile,
    TRUECALLER_ASSETS_CAR_FIX_START,
    TRUECALLER_ASSETS_CAR_FIX_END
  );
  const postIntegrateHook = /^(\s*)post_integrate\s+do\s+\|\s*installer\s*\|\s*$/m;

  if (postIntegrateHook.test(nextPodfile)) {
    return nextPodfile.replace(
      postIntegrateHook,
      (match, indent) =>
        `${match}\n${TRUECALLER_ASSETS_CAR_FIX.replace(/^/gm, `${indent}  `)}`
    );
  }

  return `${nextPodfile.trimEnd()}\n\npost_integrate do |installer|\n${TRUECALLER_ASSETS_CAR_FIX.replace(
    /^/gm,
    "  "
  )}\nend\n`;
}

const withTruecallerIOSAssetsCarOutputFix: ConfigPlugin = (config) => {
  return withPodfile(config, (config) => {
    config.modResults.contents = addTruecallerAssetsCarFix(
      config.modResults.contents
    );
    return config;
  });
};

// AppDelegate continueUserActivity forwarding is handled by
// TruecallerAppDelegateSubscriber (registered in expo-module.config.json),
// so no AppDelegate code injection is needed here.

const withTruecaller: ConfigPlugin<TruecallerPluginConfig> = (
  config,
  props
) => {
  validateProps(props);

  config = withTruecallerIOSAssetsCarOutputFix(config);

  if (props.androidClientId) {
    config = withTruecallerAndroid(config, {
      androidClientId: props.androidClientId,
    });
  }

  if (props.iosAppKey && props.iosAppLink) {
    config = withTruecallerIOSPlist(config, {
      iosAppKey: props.iosAppKey,
      iosAppLink: props.iosAppLink,
    });
    config = withTruecallerIOSAssociatedDomains(config, {
      iosAppLink: props.iosAppLink,
    });
  }

  return config;
};

export default createRunOncePlugin(withTruecaller, pkg.name, pkg.version);
