package com.timepath.vfs.provider

import com.timepath.vfs.SimpleVFile

/**
 * @author TimePath
 */
public interface ProviderPlugin {

    public fun register(): SimpleVFile.FileHandler
}
