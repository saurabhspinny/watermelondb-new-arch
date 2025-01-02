import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

import type { SerializedQuery } from '../Query'
import type { TableName, AppSchema } from '../Schema'
import type { SchemaMigrations } from '../Schema/migrations'
import type { RecordId } from '../Model'

import type {
  DatabaseAdapter,
  CachedFindResult,
  CachedQueryResult,
  BatchOperation,
  UnsafeExecuteOperations,
} from '../adapters/type'

export interface Spec extends TurboModule {
    install(successCallback: (result: boolean) => void, errorCallback: (error: string) => void): void;
    
    initialize: (
      tag: number,
      databaseName: string,
      schemaVersion: number,
      unsafeNativeReuse: boolean,
      successCallback: (result: { code: "ok" | "schema_needed" | "migrations_needed"; databaseVersion?: number }) => void,
      errorCallback: (error: string) => void
    ) => void;

    setUpWithSchema: (
      tag: number,
      databaseName: string,
      schema: string,
      schemaVersion: number,
      unsafeNativeReuse: boolean,
      successCallback: (result: void) => void,
      errorCallback: (error: string) => void
    ) => void;

    setUpWithMigrations: (
      tag: number,
      databaseName: string,
      migrations: string,
      fromVersion: number,
      toVersion: number,
      unsafeNativeReuse: boolean,
      successCallback: (result: void) => void,
      errorCallback: (error: string) => void
    ) => void;

    find(
      tag: number,
      table: TableName<any>,
      id: RecordId,
      successCallback: (result: CachedFindResult) => void,
      errorCallback: (error: string) => void
    ): void;
    
    query(
      tag: number, 
      table: string, 
      query: string, 
      args: any[],
      successCallback: (result: CachedQueryResult) => void,
      errorCallback: (error: string) => void
    ): void;
    
    queryIds(
      tag: number, 
      query: string,
      args: any[],
      successCallback: (result: RecordId[]) => void,
      errorCallback: (error: string) => void
    ): void;
  
    unsafeQueryRaw(
      tag: number, 
      query: string,
      args: any[],
      successCallback: (result: any[]) => void,
      errorCallback: (error: string) => void
    ): void;

    count(
      tag: number, 
      query: string,
      args: any[],
      successCallback: (result: number) => void,
      errorCallback: (error: string) => void
    ): void;
    
    batch(
      tag: number, 
      operations: BatchOperation[],
      successCallback: (result: void) => void,
      errorCallback: (error: string) => void
    ): void;

    unsafeResetDatabase(
      tag: number, 
      schema: string, 
      schemaVersion: number,
      successCallback: (result: void) => void,
      errorCallback: (error: string) => void
    ): void;

    unsafeGetLocalSynchronously(
      tag: number,
      key: string
    ): [string, string] | null;

    provideSyncJson(
      id: number,
      json: string,
      successCallback: (result: void) => void,
      errorCallback: (error: string) => void
    ): void;
    
    // getRandomBytes
    getRandomBytes: (count: number) => number[];
}

export default TurboModuleRegistry.getEnforcing<Spec>('WatermelonDB');
